package com.mbhoni_creative.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.api.dto.ApiEnvelope;
import com.mbhoni_creative.api.dto.WorkforceDtos.*;
import com.mbhoni_creative.api.error.WorkforceProblem;
import com.mbhoni_creative.api.repository.WorkforceRepository;
import com.mbhoni_creative.api.repository.WorkforceRepository.*;
import com.mbhoni_creative.api.security.WorkforceFieldPolicy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class WorkforceService {
    private final WorkforceRepository repository;
    private final ApiReadService identity;
    private final ObjectMapper mapper;
    public static final Map<String,String> REFERENCE_TYPES = Map.of("countries","COUNTRY","currencies","CURRENCY",
            "employment-types","EMPLOYMENT_TYPE","employee-statuses","EMPLOYEE_STATUS");
    public WorkforceService(WorkforceRepository repository, ApiReadService identity, ObjectMapper mapper) {
        this.repository=repository; this.identity=identity; this.mapper=mapper;
    }
    private long tenant() { return identity.me().tenantId(); }
    private record Visibility(List<Unit> units, Set<Long> visible) {}
    private Visibility visibility(long tenant) {
        List<Unit> units=repository.units(tenant);
        Map<Long,Unit> byId=units.stream().collect(Collectors.toMap(Unit::id,u->u));
        Map<Long,Boolean> memo=new HashMap<>();
        for (Unit unit: units) {
            Set<Long> path=new LinkedHashSet<>(); Long cursor=unit.id(); boolean allowed=true;
            while (cursor!=null) {
                if (memo.containsKey(cursor)) { allowed=memo.get(cursor); break; }
                Unit current=byId.get(cursor);
                if (current==null || current.isolated() || !path.add(cursor)) { allowed=false; break; }
                cursor=current.parent();
            }
            for (Long id:path) memo.put(id,allowed);
            if (!memo.containsKey(unit.id())) memo.put(unit.id(),false);
        }
        Set<Long> visible=memo.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
        return new Visibility(units,visible);
    }
    public ApiEnvelope<List<EmployeeSummary>> employees(int page,int size,Long org,String status,LocalDateTime since) {
        long tenant=tenant(); var visibility=visibility(tenant);
        if (org!=null && !visibility.visible().contains(org)) return page(List.of(),page,size,0);
        long count=repository.count(tenant,visibility.visible(),org,status,since);
        var rows=repository.employees(tenant,visibility.visible(),null,org,status,since,page,size);
        return page(summaries(tenant,rows),page,size,count);
    }
    public EmployeeDetail employee(long id) {
        long tenant=tenant(); var visibility=visibility(tenant);
        var rows=repository.employees(tenant,visibility.visible(),id,null,null,null,0,1);
        if (rows.isEmpty()) throw new WorkforceProblem(404,"EMPLOYEE_NOT_FOUND");
        var e=summaries(tenant,rows).getFirst();
        return new EmployeeDetail(e.id(),e.employeeNumber(),e.firstName(),e.lastName(),e.jobTitle(),e.status(),e.employmentType(),
                e.orgUnitId(),e.managerId(),e.startDate(),e.updatedAt(),e.customFields());
    }
    private static String fieldIdentity(Definition d) { return d.section()+":"+d.key(); }
    private Map<String,Definition> effective(List<Definition> definitions) {
        Map<String,Definition> effective=new LinkedHashMap<>();
        definitions.stream().filter(d->d.tenant()==null).forEach(d->effective.put(fieldIdentity(d),d));
        definitions.stream().filter(d->d.tenant()!=null).forEach(d->effective.put(fieldIdentity(d),d));
        return effective;
    }
    private boolean safe(Definition d) {
        return d.active() && WorkforceFieldPolicy.exposed(d.key(),d.label(),d.section(),d.type(),d.tenant()==null);
    }
    private List<EmployeeSummary> summaries(long tenant,List<EmployeeRow> rows) {
        if (rows.isEmpty()) return List.of();
        List<Definition> definitions=repository.definitions(tenant);
        Map<Long,Definition> byId=definitions.stream().collect(Collectors.toMap(Definition::id,d->d));
        Map<String,Definition> effective=effective(definitions);
        Map<String,Long> keyCounts=effective.values().stream().collect(Collectors.groupingBy(Definition::key,Collectors.counting()));
        Map<Long,Map<String,String>> values=new HashMap<>();
        // Apply global fallback first, then tenant values. Foreign definitions never reach this query.
        var allValues=repository.values(tenant,rows.stream().map(EmployeeRow::id).toList());
        for (boolean tenantOwned:new boolean[]{false,true}) for (Value v:allValues) {
            Definition original=byId.get(v.definition());
            if (original==null || (original.tenant()!=null)!=tenantOwned) continue;
            Definition current=effective.get(fieldIdentity(original));
            if (current==null || !safe(original) || !safe(current) || keyCounts.get(current.key())!=1 || original.type()!=current.type()) continue;
            if (v.value()!=null) values.computeIfAbsent(v.employee(),k->new TreeMap<>()).put(current.key(),v.value());
        }
        return rows.stream().map(e->new EmployeeSummary(e.id(),e.number(),e.first(),e.last(),e.title(),e.status(),e.employmentType(),
                e.org(),e.manager(),e.start(),e.updatedAt(),Map.copyOf(values.getOrDefault(e.id(),Map.of())))).toList();
    }
    public ApiEnvelope<List<Field>> fields(int page,int size) {
        long tenant=tenant();
        var rows=effective(repository.definitions(tenant)).values().stream().filter(this::safe).sorted(Comparator.comparing(Definition::id))
                .map(d->new Field(d.id(),d.key(),d.label(),d.section().name(),d.type().name(),d.required(),options(d),d.order())).toList();
        return sliced(rows,page,size);
    }
    private List<String> options(Definition d) {
        if (d.type()!=com.mbhoni_creative.adminentity.DataType.SELECT || d.options()==null) return List.of();
        try { return mapper.readValue(d.options(),new TypeReference<List<String>>(){}); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Invalid field option configuration",e); }
    }
    public ApiEnvelope<List<OrgUnit>> units(int page,int size,LocalDateTime since) {
        var visibility=visibility(tenant());
        var rows=visibility.units().stream().filter(u->visibility.visible().contains(u.id()))
                .filter(u->since==null || u.updatedAt()!=null && !u.updatedAt().isBefore(since))
                .map(u->new OrgUnit(u.id(),u.name(),u.code(),u.parent(),u.type(),u.updatedAt())).toList();
        return sliced(rows,page,size);
    }
    public ApiEnvelope<List<Reference>> references(String type,int page,int size) {
        String category=REFERENCE_TYPES.get(type);
        if (category==null) throw new WorkforceProblem(404,"REFERENCE_TYPE_NOT_FOUND");
        return sliced(repository.references(tenant(),category),page,size);
    }
    private static <T> ApiEnvelope<List<T>> sliced(List<T> rows,int page,int size) {
        long offset=(long)page*size;
        return page(offset>=rows.size()?List.of():rows.subList((int)offset,(int)Math.min(offset+size,rows.size())),page,size,rows.size());
    }
    private static <T> ApiEnvelope<List<T>> page(List<T> rows,int page,int size,long count) {
        return new ApiEnvelope<>(rows,new ApiEnvelope.Page(page,size,count));
    }
}
