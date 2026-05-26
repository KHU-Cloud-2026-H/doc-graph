package com.docgraph.backend.acceptance

import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("notion-live-e2e")
class NotionLiveE2EController {

    @GetMapping("/notion-live-e2e", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(): String = """
        <!doctype html>
        <html lang="ko">
        <head>
          <meta charset="utf-8" />
          <title>DocGraph Notion Live E2E</title>
          <style>
            body { font-family: system-ui, sans-serif; max-width: 960px; margin: 32px auto; line-height: 1.5; }
            button, a.button { display: inline-block; border: 1px solid #222; border-radius: 6px; padding: 8px 12px; background: #fff; color: #111; text-decoration: none; cursor: pointer; }
            pre { background: #f6f8fa; border: 1px solid #d0d7de; border-radius: 6px; padding: 12px; overflow: auto; }
            .row { display: flex; gap: 8px; align-items: center; margin: 12px 0; }
            input { padding: 8px; min-width: 280px; }
          </style>
        </head>
        <body>
          <h1>DocGraph Notion Live E2E</h1>
          <p>1. Notion 인증 후 이 페이지로 돌아오면, 서버가 저장한 workspace, Notion page 목록, 선택된 page의 block/text를 같은 쿠키 세션으로 조회합니다.</p>
          <p><a class="button" href="/api/oauth2/authorization/notion">Notion 인증 시작</a></p>
          <div class="row">
            <input id="query" placeholder="page 검색어 optional" />
            <button onclick="run()">워크스페이스/page/block 조회</button>
            <button onclick="syncFirstPage()">첫 page 서버 저장 동기화</button>
          </div>
          <pre id="output">ready</pre>
          <script>
            const out = document.getElementById('output');
            const pretty = (v) => JSON.stringify(v, null, 2);
            async function getJson(path) {
              const res = await fetch(path, { credentials: 'include' });
              const text = await res.text();
              if (!res.ok) throw new Error(path + ' -> ' + res.status + '\n' + text);
              return text ? JSON.parse(text) : null;
            }
            async function run() {
              out.textContent = 'running...';
              try {
                const me = await getJson('/api/auth/me');
                const workspaces = await getJson('/api/workspaces');
                const workspace = workspaces[0];
                let pages = [];
                if (workspace) {
                  const query = document.getElementById('query').value;
                  const suffix = query ? '?query=' + encodeURIComponent(query) : '';
                  pages = await getJson('/api/workspaces/' + workspace.id + '/notion/pages' + suffix);
                }
                const pageContents = [];
                if (workspace && pages.length > 0) {
                  for (const page of pages.slice(0, 3)) {
                    pageContents.push(await getJson('/api/workspaces/' + workspace.id + '/notion/pages/' + page.id + '/content'));
                  }
                }
                out.textContent = pretty({ me, workspaces, pages, pageContents });
              } catch (e) {
                out.textContent = String(e);
              }
            }
            async function postJson(path, body) {
              const res = await fetch(path, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: body ? JSON.stringify(body) : undefined,
              });
              const text = await res.text();
              if (!res.ok) throw new Error(path + ' -> ' + res.status + '\n' + text);
              return text ? JSON.parse(text) : null;
            }
            async function waitForStoredDocument(projectId, notionPageId) {
              for (let i = 0; i < 20; i++) {
                const list = await getJson('/api/projects/' + projectId + '/documents?size=50');
                const doc = list.content.find((item) => item.notionPageId === notionPageId);
                if (doc) {
                  return {
                    summary: doc,
                    detail: await getJson('/api/documents/' + doc.id),
                  };
                }
                await new Promise((resolve) => setTimeout(resolve, 1000));
              }
              throw new Error('stored document not found after sync');
            }
            async function syncFirstPage() {
              out.textContent = 'syncing first page into server DB...';
              try {
                const me = await getJson('/api/auth/me');
                const workspaces = await getJson('/api/workspaces');
                const workspace = workspaces[0];
                if (!workspace) throw new Error('workspace not found');
                const pages = await getJson('/api/workspaces/' + workspace.id + '/notion/pages');
                const page = pages[0];
                if (!page) throw new Error('page not found');
                const existingProjects = await getJson('/api/workspaces/' + workspace.id + '/projects');
                let project = existingProjects[0];
                if (!project) {
                  project = await postJson('/api/workspaces/' + workspace.id + '/projects', {
                    name: 'notion-live-e2e-' + Date.now(),
                    notionRootPageId: page.id,
                  });
                }
                await postJson('/api/projects/' + project.id + '/sync');
                const storedDocument = await waitForStoredDocument(project.id, page.id);
                out.textContent = pretty({ me, workspace, sourcePage: page, project, storedDocument });
              } catch (e) {
                out.textContent = String(e);
              }
            }
            run();
          </script>
        </body>
        </html>
    """.trimIndent()
}
