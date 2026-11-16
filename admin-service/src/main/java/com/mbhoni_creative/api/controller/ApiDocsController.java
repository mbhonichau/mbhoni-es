package com.mbhoni_creative.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Hidden
public class ApiDocsController {
    @GetMapping(value = {"/api/docs", "/api/docs/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String docs(jakarta.servlet.http.HttpServletRequest request) {
        String spec = org.springframework.web.util.HtmlUtils.htmlEscape(request.getContextPath() + "/api/openapi.json");
        return """
            <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width">
            <title>MBHONI Admin API v1</title>
            <style>body{font:16px system-ui;max-width:900px;margin:3rem auto;padding:1rem}pre{white-space:pre-wrap}input{width:25rem;max-width:90%%}button{margin:1rem}article{border-top:1px solid #ddd;padding:1rem 0}</style>
            <h1>MBHONI Admin API v1</h1><p>Use your tenant API key to read the OpenAPI specification. The key is used for this request only and is not saved.</p>
            <form id="docs"><label>API key <input id="key" type="password" autocomplete="off" required></label><button>Load documentation</button></form>
            <p id="message" role="status"></p><main id="endpoints"></main><details><summary>OpenAPI JSON</summary><pre id="spec"></pre></details>
            <script>
            document.getElementById('docs').addEventListener('submit',async e=>{
              e.preventDefault();const input=document.getElementById('key');const key=input.value;input.value='';
              const message=document.getElementById('message');const target=document.getElementById('endpoints');
              target.replaceChildren();document.getElementById('spec').textContent='';
              try{const response=await fetch('%s',{headers:{'X-API-Key':key},credentials:'omit'});
                if(!response.ok){message.textContent='Unable to load specification ('+response.status+').';return;}
                const spec=await response.json();message.textContent='Specification loaded.';
                document.getElementById('spec').textContent=JSON.stringify(spec,null,2);
                for(const [path,methods] of Object.entries(spec.paths||{}))for(const [method,op] of Object.entries(methods)){
                  const article=document.createElement('article');const title=document.createElement('h2');
                  title.textContent=method.toUpperCase()+' '+path;article.append(title);
                  const description=document.createElement('p');description.textContent=op.summary||op.operationId||'';article.append(description);target.append(article);
                }
              }catch(error){message.textContent='Documentation could not be loaded.';}
            });
            </script></html>
            """.formatted(spec);
    }
}
