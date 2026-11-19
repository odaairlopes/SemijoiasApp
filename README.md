# Semi-joias App

App Android que acelera a divulgação de produtos de uma loja de semi-joias no WhatsApp. Ele abre o painel administrativo da loja e adiciona, em cada produto, botões para compartilhar **foto + descrição + preço + link** com um toque.

> Aplicativo **híbrido**: uma `WebView` hospeda o painel web existente e uma ponte nativa (Java) cuida do que a web não consegue fazer, como baixar a imagem e abrir o compartilhamento do WhatsApp com anexo.

## O problema

O painel da loja não tem compartilhamento. Para divulgar um produto, era preciso baixar a foto, copiar manualmente código, descrição, tamanho e valor, abrir o WhatsApp, anexar a imagem e colar o texto. Isso se repetia dezenas de vezes por dia.

## A solução

| Recurso | O que faz |
| --- | --- |
| **Compartilhar no WhatsApp** | Baixa a foto do produto e abre o WhatsApp (ou WhatsApp Business) com a imagem anexada e a mensagem formatada |
| **Copiar mensagem / link** | Copia o texto pronto ou o link do produto para a área de transferência |
| **Envio em lote** | Marque vários produtos e envie todos em uma única mensagem |
| **Mensagem padronizada** | Emoji por categoria (anel, brinco, colar...), descrição, tamanho, valor e link |

## Arquitetura

```
WebView (painel da loja)
   │  onPageFinished (apenas domínio confiável)
   ▼
assets/inject.js ── lê o DOM dos cards, injeta botões e observa novas páginas (MutationObserver)
   │  AndroidShare.shareImageToWhatsApp(url, mensagem)
   ▼
ShareBridge (@JavascriptInterface)
   ├─ ImageDownloader ── HTTPS, timeouts, valida image/*, limite de 10 MB
   └─ FileProvider + Intent.ACTION_SEND ── WhatsApp → fallback WhatsApp Business
```

| Arquivo | Responsabilidade |
| --- | --- |
| `MainActivity` | Configura a WebView, cookies, botão voltar e o ciclo de vida |
| `StoreConfig` | URL inicial e regra de origem confiável |
| `ShareBridge` | Ponte JS → Android e abertura do compartilhamento |
| `ImageDownloader` | Download seguro da imagem para o cache |
| `assets/inject.js` | Script injetado na página (separado do Java para facilitar manutenção) |

### Decisões e trade-offs

- **WebView em vez de reescrever o painel.** O painel pertence a uma plataforma de terceiros. Estender a interface existente entrega valor rápido, ao custo de depender da estrutura do HTML deles (seletores como `#ProductsList .card`). Se a plataforma mudar o layout, o `inject.js` precisa de ajuste.
- **Script em `assets/`, não em string Java.** Fica legível, com destaque de sintaxe e validável com `node --check`.
- **Segurança da ponte JavaScript.** Um `@JavascriptInterface` é visível a qualquer página carregada na WebView. Por isso:
  - navegação para fora do domínio `semijoias.net` abre no app externo, não na WebView;
  - o script só é injetado em páginas HTTPS confiáveis;
  - a ponte ignora chamadas quando a página atual não é confiável;
  - o download aceita só HTTPS, exige `Content-Type: image/*` e limita o tamanho.
- **Download fora da main thread** com `ExecutorService`, e retorno à UI via `Handler`.
- **`FileProvider`** para entregar a imagem ao WhatsApp sem expor o sistema de arquivos (`content://` com permissão temporária de leitura).
- **`usesCleartextTraffic="false"`**: todo o tráfego é HTTPS.

## Stack

- Java 8, Android SDK (minSdk 24, targetSdk 34)
- AndroidX AppCompat, Material Components
- WebView, `@JavascriptInterface`, FileProvider, Intents
- JavaScript (DOM, `MutationObserver`, Clipboard API)
- Gradle 8.2 / Android Gradle Plugin 8.2

## Como rodar

Pré-requisitos: Android Studio (ou Android SDK) e JDK 17.

```bash
git clone https://github.com/odaairlopes/SemijoiasApp.git
cd SemijoiasApp
./gradlew assembleDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`. Para instalar em um dispositivo conectado:

```bash
./gradlew installDebug
```

> O app abre o painel administrativo da loja, que exige login. O login é feito pelo próprio site dentro da WebView; **nenhuma credencial é armazenada no código**.

## Próximos passos

- Migrar para Kotlin.
- Testes unitários para `StoreConfig` e `ImageDownloader`, e teste do `inject.js` com Jest + jsdom.
- Compartilhar várias imagens no envio em lote (`ACTION_SEND_MULTIPLE`).
- Configurar o domínio da loja por `BuildConfig`/flavors para reutilizar o app em outras lojas.
- Pipeline de CI (GitHub Actions) com build e lint.

## Autor

Odair Lopes S. Andrade
