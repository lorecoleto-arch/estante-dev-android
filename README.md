# Estante Dev 📚💻 — Trabalho Final (Jetpack Compose)

**Aluna:** Lorena Coleto — RA 842835 · **Projeto:** Estante de Livros

> **Parte final do projeto.** Este repositório é a **continuação da entrega parcial**: o mesmo tema
> (estante de livros de programação) agora com toda a interface em Jetpack Compose.
>
> | Etapa | O que foi feito | Onde está |
> |---|---|---|
> | Parcial | 2 telas em Android Views (XML), Intent explícita e dados simulados | tag [`parcial`](../../tree/parcial) |
> | **Final (esta entrega)** | 4 telas em Compose, Navigation 3, `ViewModel`, coroutines, API e dados salvos no aparelho | branch `main` |

## Objetivo

Aplicativo Android para quem estuda **Engenharia de Software** organizar a leitura de **livros de
programação**. Você busca os livros no catálogo público da [Open Library](https://openlibrary.org),
vê os detalhes e registra na sua estante se **quer ler**, está **lendo** (com páginas lidas) ou
**já leu** (com nota e anotação). A estante fica salva no aparelho e continua disponível sem internet.

Toda a interface é feita em **Jetpack Compose**, com uma única `Activity`.

---

## Como compilar e executar

### Pré-requisitos
- **Android Studio** recente, compatível com o Android Gradle Plugin **9.4** (o projeto foi testado no
  Android Studio Quail 4 — 2026.1.4). O Java necessário (17 ou mais novo) já vem com ele.
- **Android SDK Platform 37**. Se não estiver instalado, o Android Studio oferece o download na
  sincronização; também dá para instalar em `Settings > Languages & Frameworks > Android SDK`.
- Emulador ou celular com **Android 8.0 (API 26)** ou superior, com **internet** para buscar livros.
- Internet no computador na primeira compilação, para baixar as dependências.

**Não é preciso configurar nenhuma chave, senha ou arquivo `secrets.properties`:** a Open Library é
pública e não exige autenticação.

### Pelo Android Studio
1. `File > Open…` e selecione a pasta do projeto (a que contém `settings.gradle.kts`).
2. Espere o *Gradle Sync* terminar.
3. Escolha a configuração **`app`** e um emulador ou celular.
4. Clique em **Run ▶**. O app aparece com o nome **“Estante Dev”**.

### Pela linha de comando
Com o `JAVA_HOME` apontando para o Java 17 ou mais novo e o SDK indicado em `ANDROID_HOME`
(ou em `local.properties`):

```bash
./gradlew :app:installDebug      # compila e instala no emulador/celular conectado
./gradlew test                   # roda os testes automatizados (sem emulador)
```
No Windows use `gradlew.bat` no lugar de `./gradlew`.

---

## Telas

1. **Buscar** — digite título, autor ou tecnologia, ou toque num tema sugerido (Clean Code,
   Algoritmos, Design Patterns…). Na primeira vez abre com “clean code”; depois repete a última busca.
2. **Detalhes** — capa, dados do livro, descrição, botão de compartilhar e a sua leitura.
3. **Adicionar/editar leitura** — formulário com validação: “Lendo” exige as páginas lidas, “Lido”
   exige nota de 1 a 5 e a anotação é opcional.
4. **Minha estante** — os livros salvos, com filtros por situação.

A pilha de navegação liga todas: Buscar → Detalhes → Formulário → Detalhes, e a estante também abre
os detalhes de cada livro.

---

## Onde está cada item avaliado

| Item | Onde |
|---|---|
| Três ou mais telas em Compose, sem layouts XML | 4 telas: `SearchScreen`, `BookDetailScreen`, `EntryEditorScreen`, `ShelfScreen`. Não existe pasta `res/layout` |
| `LazyColumn` / `LazyRow` | `SearchScreen` (resultados e temas) e `ShelfScreen` (livros e filtros) |
| `ViewModel` | `SearchViewModel`, `BookDetailViewModel`, `EntryEditorViewModel`, `ShelfViewModel` — expõem `StateFlow`, coletado com `collectAsStateWithLifecycle` |
| Operação assíncrona com coroutines | Busca na API com *debounce* e cancelamento (`SearchViewModel.launchSearch`), descrição (`BookDetailViewModel.loadDescription`), gravação no banco (`EntryEditorViewModel.persist`) |
| Feedback de cada estado | Tabela abaixo |
| Navigation 3 em uma única `Activity` | `MainActivity`, `ui/navigation/Routes.kt` (rotas com argumento) e `ui/EstanteApp.kt` (`NavDisplay`, pilha e entries) |
| Valores opcionais (`String?`, `Int?`) | `Book`, `ShelfEntry` e os DTOs. Ano, páginas, capa, descrição, nota e anotação podem faltar, e cada um tem um texto ou imagem alternativa. O código do app não usa `!!` |
| `kotlinx.serialization` | JSON da API → DTOs em `data/remote/OpenLibraryApi.kt` (conversor do Retrofit em `AppContainer`); rotas `@Serializable` da navegação |
| Sem dados sensíveis no repositório | Não há credenciais. `local.properties`, `secrets.properties` e `.env` estão no `.gitignore` |
| Sem dados sensíveis nos logs | O código do app não escreve no Logcat |

### Estados das operações assíncronas

| Operação | Carregando | Sucesso | Vazio | Erro |
|---|---|---|---|---|
| Buscar livros | “Buscando livros…” | Lista com a quantidade de resultados | “Nenhum livro encontrado” | Mensagem conforme a causa + “Tentar de novo”; se houver resultado salvo, mostra-o com um aviso |
| Abrir detalhes | “Carregando livro…” | Dados do livro | Livro não encontrado | “Tentar de novo” |
| Carregar descrição | “Carregando descrição…” | Texto | Aviso de que não há descrição | Mensagem + “Tentar de novo” |
| Abrir formulário | “Carregando formulário…” | Campos preenchidos | Livro não encontrado | “Tentar de novo” |
| Salvar / remover leitura | Indicador no botão | Mensagem de confirmação | — | Aviso no topo, sem perder o que foi digitado |
| Abrir a estante | “Carregando sua estante…” | Lista | “Sua estante está vazia” ou filtro sem livros | “Tentar de novo” |

---

## Arquitetura

```
UI (Compose)  ──eventos──▶  ViewModel  ──▶  Repository  ──▶  Room / DataStore (local)
     ▲                         │                  └────────▶  Retrofit / Open Library (remoto)
     └──── StateFlow<UiState> ─┘
```

- **Fluxo unidirecional de dados:** a tela mostra o `UiState` e avisa o ViewModel dos eventos
  (`onQueryChange`, `onSave`…). Só o ViewModel muda o estado, e ele sobrevive à rotação.
- **Repository:** `BookRepository` (busca + cache) e `ShelfRepository` (estante) são interfaces, com
  implementações reais (`Default…`) e *fakes* nos testes.
- **Injeção de dependências manual:** `AppContainer` cria as dependências uma vez, em `EstanteApplication`.
- **DTO → domínio:** `BookDocDto`/`WorkDto` espelham o JSON da API e são convertidos para `Book`.
- **Cache:** o resultado de cada busca vale por **1 hora** no Room. Depois disso (ou em “Atualizar” e
  “Tentar de novo”) a API é consultada; se ela falhar, o app usa o que está salvo.
- **Nova tentativa:** a Open Library às vezes demora ou falha; nesses casos o app tenta mais uma vez
  sozinho antes de mostrar o erro.
- **Cancelamento:** as operações rodam no `viewModelScope`. Uma busca nova cancela a anterior, e
  `CancellationException` nunca é engolida nos `try/catch`.

```
app/src/main/java/com/estante/app/
├── AppContainer.kt, EstanteApplication.kt, MainActivity.kt
├── domain/        modelos imutáveis (Book, ShelfEntry…) e regras do formulário (ShelfEntryValidator)
├── data/          repositories
│   ├── remote/    Retrofit (OpenLibraryApi), DTOs e mapeamento
│   ├── local/     Room (entidades, DAOs, banco)
│   └── preferences/ DataStore (última busca, filtro da estante)
└── ui/
    ├── navigation/ rotas do Navigation 3
    ├── common/     componentes reutilizáveis e estados de tela
    ├── search/ detail/ editor/ shelf/   uma pasta por tela (Screen + ViewModel)
    └── theme/
```

---

## Testes

`./gradlew test` roda **45 testes** na JVM (os de interface usam Robolectric, sem emulador):

- `ShelfEntryValidatorTest` — regras do formulário;
- `DefaultBookRepositoryTest` — cache, expiração, modo offline e erros;
- `OpenLibraryRemoteDataSourceTest` — nova tentativa quando a API falha de forma passageira;
- `MappersTest` — JSON → DTO → domínio, com campos ausentes;
- `EstanteDatabaseTest` — Room de verdade (banco em memória);
- `SearchViewModelTest`, `EntryEditorViewModelTest` — estados, debounce/cancelamento e validação;
- `AsyncStatesTest` — carregando, vazio, erro e “tentar de novo” nas demais telas;
- `ComponentsTest` — componentes Compose e estados de erro/vazio;
- `EstanteAppFlowTest` — fluxo completo na interface: buscar → detalhes → formulário → salvar → estante.

---

## Bibliotecas externas

| Biblioteca | Para quê |
|---|---|
| **Jetpack Compose** (Material 3, ícones, navigation-suite) | Toda a interface |
| **Navigation 3** | Navegação com uma pilha de rotas controlada pelo app |
| **Lifecycle** (ViewModel, runtime-compose, viewmodel-navigation3) | `ViewModel`, `collectAsStateWithLifecycle` e um ViewModel por tela da pilha |
| **Kotlinx Serialization** | Converter o JSON da API em classes Kotlin e serializar as rotas |
| **Retrofit** + **OkHttp** | Chamadas HTTP à API da Open Library |
| **Room** + **KSP** | Banco de dados local e geração do código dele |
| **DataStore Preferences** | Última busca e filtro da estante |
| **Coil** | Carregar as capas dos livros |
| **JUnit, Coroutines Test, Robolectric, Compose UI Test** | Testes automatizados |

Dados de livros e capas: [Open Library](https://openlibrary.org/developers/api) (Internet Archive).
