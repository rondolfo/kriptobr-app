#!/usr/bin/env bash
# Descobre sozinho em qual pasta do repositório está o projeto do app.
#
# Por que isto existe: os arquivos chegam aqui por arrastar-e-soltar no site do
# GitHub, e dependendo de onde a pasta é solta eles caem em "enviar/",
# "enviar/enviar/", "enviar-github_1/enviar/"... Em vez de exigir que o caminho
# seja sempre exato, o build procura. Entre as cópias, vale a que tem mais
# arquivos .kt — que é sempre a mais nova, porque cada versão só acrescenta.
melhor=""
maior=-1
for s in $(find . -name settings.gradle.kts -not -path "./.git/*"); do
  d=$(dirname "$s")
  n=$(find "$d/app/src/main" -name '*.kt' 2>/dev/null | wc -l)
  echo "  $n arquivos .kt em $d"
  if [ "$n" -gt "$maior" ]; then
    maior="$n"
    melhor="$d"
  fi
done

if [ -z "$melhor" ]; then
  echo "Nenhum projeto Gradle encontrado no repositorio."
  exit 1
fi

echo "==> Compilando $melhor ($maior arquivos .kt)"
export PROJETO="$melhor"
echo "PROJETO=$melhor" >> "$GITHUB_ENV"
