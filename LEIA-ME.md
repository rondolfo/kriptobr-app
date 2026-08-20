# KriptoBR Mercado — aplicativo Android

App do painel `mercado.kriptobr.com`, com **widget de cotação na tela inicial** e
**notificações** para cupons e avisos.

O que já está pronto neste projeto:

| Recurso | Situação |
|---|---|
| App abrindo o painel em tela cheia, sem barra de navegador | pronto |
| Puxar para atualizar, botão voltar, tela de "sem conexão" | pronto |
| Widget da tela inicial com Bitcoin e Ethereum | pronto |
| Ícone, cores e splash da marca | pronto |
| Português, inglês e espanhol | pronto |
| Notificações (Firebase) | código pronto, falta você criar o projeto no Firebase |

---

## 1. Colocar no GitHub

1. Entre em <https://github.com/new>
2. Nome do repositório: `kriptobr-app` · deixe **Private** se preferir · **não** marque
   "Add a README file"
3. Clique em **Create repository**
4. Na tela seguinte, clique em **uploading an existing file**
5. Arraste **todo o conteúdo** da pasta `kriptobr-app` (as pastas `app`, `.github`
   e os arquivos soltos `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`,
   `.gitignore`, `LEIA-ME.md`)
6. Clique em **Commit changes**

> A pasta `.github` começa com ponto e o Windows às vezes esconde esse tipo de pasta.
> Se ela não aparecer para arrastar, ligue "Itens ocultos" na aba Exibir do Explorador.
> **Sem essa pasta o APK não é gerado.**

## 2. Pegar o APK

Assim que o envio terminar, o robô de compilação começa sozinho.

1. Abra a aba **Actions** do repositório
2. Espere o item "Compilar APK" ficar verde (leva de 3 a 6 minutos na primeira vez)
3. Vá na aba **Releases** (coluna da direita, na página inicial do repositório)
4. Baixe o arquivo `KriptoBR-Mercado-v1.apk` **pelo celular**
5. Toque no arquivo baixado. O Android vai avisar que é de fora da Play Store —
   autorize. Isso é normal para app de teste.

Cada novo envio ao repositório gera um APK novo automaticamente.

## 3. Colocar o widget na tela inicial

Segure o dedo num espaço vazio da tela inicial → **Widgets** → procure
**KriptoBR Mercado** → arraste o widget "Cotação do Bitcoin" para a tela.

- Atualiza sozinho a cada 30 minutos (é o mínimo que o Android permite, para poupar bateria)
- Tocar no corpo do widget abre o painel
- Tocar na linha de baixo atualiza na hora
- Mostra em reais no Brasil e em dólar no resto do mundo

## 4. Ligar as notificações (para os cupons)

Isso depende de uma conta sua, então precisa ser você:

1. Entre em <https://console.firebase.google.com> e clique em **Criar projeto**
   (nome sugerido: `KriptoBR`). Pode desativar o Google Analytics se quiser,
   mas **mantendo ligado** você ganha a segmentação por tempo de instalação —
   que é justamente o que você quer para o cupom.
2. Dentro do projeto, clique no ícone do **Android**
3. Em "Nome do pacote Android" digite exatamente: `com.kriptobr.mercado`
4. Baixe o arquivo **`google-services.json`** que ele oferece
5. No GitHub, entre na pasta `app` do repositório → **Add file** → **Upload files**
   → arraste o `google-services.json` → **Commit changes**

Pronto. O próximo APK já sai com push funcionando.

### Enviar o cupom para quem tem o app há X dias

No Firebase Console → **Engage** → **Messaging** → **Nova campanha** → Notificações:

- Escreva título e texto
- Em **Segmentação**, escolha o app e adicione a condição
  *"Primeira abertura" — há mais de X dias*
- Em **Opções adicionais → Dados personalizados**, adicione a chave `link` com a
  URL de destino (ex.: `https://kriptobr.com/cupom-app`). O app abre direto nela
  quando a pessoa tocar na notificação.

Nenhum servidor seu envolvido — é tudo pelo painel do Firebase, de graça.

---

## Depois: publicar na Play Store

1. Conta de desenvolvedor Google Play — **US$ 25**, pagamento único
   (<https://play.google.com/console>)
2. Trocar `assembleDebug` por `bundleRelease` no arquivo `.github/workflows/android.yml`
   e configurar a assinatura (posso fazer quando chegar a hora)
3. Precisa de: política de privacidade publicada, capturas de tela, ícone 512×512
   (já está em `app/src/main/res/mipmap-xxxhdpi/`) e a descrição da loja

## Sobre o iPhone

O app de iOS não pode ser compilado a partir deste projeto: a Apple exige um
computador com macOS e o Xcode, e a conta de desenvolvedor custa **US$ 99 por ano**.

Vale saber de antemão: a Apple rejeita aplicativos que são só um site embrulhado
(a regra 4.2 das diretrizes). Para passar, o app de iPhone precisa de recurso
nativo de verdade — no nosso caso, o widget da tela de bloqueio e as notificações
resolvem isso, mas é um trabalho separado, escrito em Swift.

**Sugestão:** faça o Android primeiro, veja quanta gente instala e usa, e só então
decida se compensa o investimento no iOS.

---

## Estrutura do projeto

```
app/src/main/
  java/com/kriptobr/mercado/
    TelaPrincipal.kt    tela do app (WebView com o painel)
    WidgetCotacao.kt    widget da tela inicial
    Cotacao.kt          busca o preço no CoinGecko
    Avisos.kt           monta as notificações
    ServicoPush.kt      recebe as campanhas do Firebase
  res/
    layout/             telas e o desenho do widget
    values/             cores, textos e tema (values-en e values-es traduzidos)
    mipmap-*/           ícones do app
.github/workflows/android.yml   robô que compila o APK
```
