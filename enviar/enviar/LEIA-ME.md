# KriptoBR Mercado — aplicativo Android

App nativo do painel `mercado.kriptobr.com`. **Não é o site dentro de uma moldura** —
as telas são desenhadas em Jetpack Compose, abrem instantâneas e funcionam sem
internet com o último preço guardado.

## O que tem

| Tela | O que faz |
|---|---|
| **Mercado** | Bitcoin em destaque com mini-gráfico, Ethereum e Solana, termômetro de medo e ganância, e a lista de moedas que você escolher |
| **Alertas** | Você cria: moeda, "acima de" ou "abaixo de", valor. O app confere a cada 15 minutos mesmo fechado e avisa quando bate |
| **Painel** | O site completo, para o raio-X da rede, liquidações e conversor |
| **Loja** | Hardware wallets, KriptoSteel, cursos e o KriptoHoje |

**Widget da tela inicial:** preço do Bitcoin, **variação de 24 h**, Ethereum na
linha de baixo e **bolinha de atualizar** no canto. Mostra sempre o último preço
guardado — nunca mais fica em branco escrito "sem conexão".

Português, inglês e espanhol. Em reais no Brasil, em dólar no resto do mundo.

## Por que o widget quebrava antes

O Android dá **10 segundos** para um widget terminar o que começou. A versão
anterior buscava o preço dentro do próprio widget, com até 24 segundos de espera —
quando a internet demorava, o sistema matava o processo no meio.

Agora quem busca da internet é o WorkManager, que pode demorar o quanto precisar,
e o widget só desenha o que já está guardado. São duas coisas separadas, e é por
isso que não trava mais.

## Pegar o APK

O robô de compilação já está no repositório. A cada envio ele gera um APK novo em
**Releases**, em 3 a 6 minutos. Baixe pelo celular e toque no arquivo.

## Notificações da KriptoBR (cupons) — já está ligado

O projeto **KriptoBR Mercado** (`kriptobr-mercado`) já existe no Firebase e o
`app/google-services.json` já está nesta pasta, com os dois pacotes registrados:

| Pacote | Para que serve |
|---|---|
| `com.kriptobr.mercado` | versão de release (Play Store) |
| `com.kriptobr.mercado.debug` | o APK de teste que sai do robô |

Não precisa baixar nem mover arquivo nenhum. O próximo APK já sai com push e com
relatório de falhas (Crashlytics) funcionando.

Os alertas de preço que o próprio usuário cria continuam funcionando sem Firebase.

### Enviar o cupom

Firebase Console → **Engage** → **Messaging** → **Nova campanha**:

- Título e texto da notificação
- Em **Segmentação**, condição *"Primeira abertura" — há mais de X dias*
- Em **Dados personalizados**, chave `link` com a URL do cupom
  (ex.: `https://kriptobr.com/cupom-app`) — o app abre direto nela

Sem servidor seu no meio.

## Depois: Play Store

1. Conta de desenvolvedor — **US$ 25**, uma vez só (<https://play.google.com/console>)
2. Trocar `assembleDebug` por `bundleRelease` no `.github/workflows/android.yml`
   e configurar a assinatura
3. Precisa de política de privacidade publicada, capturas de tela e o ícone 512×512
   (já está em `app/src/main/res/mipmap-xxxhdpi/`)

## Sobre o iPhone

Não sai deste projeto: a Apple exige macOS com Xcode e conta de US$ 99 por ano.
A boa notícia é que agora o app tem recurso nativo de verdade (widget e alertas),
então não cairia na regra 4.2, que rejeita site embrulhado. Mas é um trabalho
separado, escrito em Swift.

## Estrutura

```
app/src/main/java/com/kriptobr/mercado/
  MainActivity.kt          abas e estado geral
  Avisos.kt                notificações
  ServicoPush.kt           campanhas do Firebase
  dados/                   API, cache, favoritos, formatação
  ui/                      as quatro telas + peças e tema
  alerta/                  modelo do alerta e o verificador de 15 em 15 min
  widget/                  widget da tela inicial
```
