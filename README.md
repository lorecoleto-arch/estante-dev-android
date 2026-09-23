# Estante Dev 📚💻

**Aluna:** Lorena Coleto — RA 842835 · **Projeto:** Estante de Livros

Aplicativo Android para quem estuda **Engenharia de Software** organizar a leitura de **livros de
programação** — *Código Limpo*, *Entendendo Algoritmos*, *Padrões de Projeto*, livros de lógica,
Kotlin, arquitetura… Você busca os livros no catálogo público da
[Open Library](https://openlibrary.org), vê os detalhes e registra na sua estante se **quer ler**,
está **lendo** (com páginas lidas) ou **já leu** (com nota e anotação). A estante fica salva no
aparelho e continua lá depois de fechar o app, inclusive sem internet.

O repositório tem **dois módulos de aplicativo**, um para cada etapa da disciplina, os dois sobre o
mesmo tema:

| Módulo | Etapa | Tecnologia | O que faz |
|---|---|---|---|
| `views` | Parcial | Android Views (XML) + Intent | Lista de livros clássicos de programação (dados simulados) → detalhes → marcar como lido e dar nota |
| `app` | Etapa 2 | Jetpack Compose | App completo: busca na API → detalhes → formulário de leitura → estante salva localmente |

---

## Como rodar

### Requisitos
- **Android Studio** recente (com suporte ao Android Gradle Plugin 9.4). O JDK 17+ já vem junto (JBR).
- Um emulador ou celular com **Android 8.0 (API 26)** ou superior.
- **Internet** no emulador/celular para a busca do módulo `app` (o módulo `views` funciona offline).

> Não há chaves de API, senhas nem arquivo `.env`: a Open Library é pública e não exige autenticação.

### Pelo Android Studio
1. `File > Open…` e selecione a pasta do projeto.
2. Espere o *Gradle Sync* terminar (na primeira vez ele baixa as dependências e, se faltar, o SDK 37).
3. Na barra de cima, escolha a configuração de execução:
   - **`app`** → Etapa 2 (Compose) — ícone **“Estante Dev”**;
   - **`views`** → Parcial (Views/XML) — ícone **“Estante Dev (Views)”**.
4. Clique em **Run ▶**.

### Pela linha de comando
```bash
./gradlew :app:installDebug      # instala a Etapa 2 (Compose)
./gradlew :views:installDebug    # instala a Parcial (Views)
./gradlew test                   # roda todos os testes automatizados (na JVM, sem emulador)
```
No Windows use `gradlew.bat` no lugar de `./gradlew`.

---

## Fluxo principal (módulo `app`)

1. **Buscar** — digite título, autor ou tecnologia, ou toque num dos **temas sugeridos**
   (Clean Code, Algoritmos, Lógica de programação, Design Patterns, Engenharia de software, Kotlin,
   Arquitetura). A busca é feita na API da Open Library (com *debounce*: espera você parar de
   digitar). A última busca fica salva e é repetida ao abrir o app — na primeira vez, “clean code”.
2. **Resultados** — lista (`LazyColumn`) com capa, autor e ano; livros que já estão na estante
   mostram um selo (“Lendo”, “Lido”…).
3. **Detalhes** — capa, dados do livro, descrição (carregada de forma assíncrona) e botão de
   compartilhar.
4. **Adicionar/editar leitura** — formulário com validação:
   - “Lendo” exige páginas lidas (número entre 0 e o total do livro);
   - “Lido” exige nota de 1 a 5;
   - anotação opcional com até 500 caracteres.
5. **Minha estante** — lista reativa dos livros salvos, com filtros por situação (`LazyRow`).
   Tudo fica no banco local (Room) e continua lá ao reiniciar o app.

Sem internet, a busca mostra os **resultados já salvos** daquele termo (com um aviso); se não houver
nada salvo, mostra o estado de **erro** com “Tentar de novo”.

---

## Arquitetura (módulo `app`)

```
UI (Compose)  ──eventos──▶  ViewModel  ──▶  Repository  ──▶  Room / DataStore (local)
     ▲                         │                  └────────▶  Retrofit / Open Library (remoto)
     └──── StateFlow<UiState> ─┘
```

- **Fluxo unidirecional de dados:** a tela só *mostra* o `UiState` e *avisa* o ViewModel dos eventos
  (`onQueryChange`, `onSave`…). O ViewModel é o único que muda o estado.
- **Estados de tela:** cada tela tem um `UiState` com casos distintos — carregando, conteúdo,
  lista vazia e erro — e o ViewModel sobrevive à rotação, então nada se perde ao girar o celular.
- **Repository:** `BookRepository` (busca + cache) e `ShelfRepository` (estante). São interfaces, com
  implementações reais (`Default…`) e *fakes* nos testes.
- **Injeção de dependências manual:** `AppContainer` cria tudo uma vez (`EstanteApplication`) e as
  telas recebem só as interfaces.
- **DTO → domínio:** `BookDocDto`/`WorkDto` espelham o JSON da API e são convertidos para `Book`
  (limpando Markdown da descrição, assuntos repetidos, campos ausentes).
- **Cache com expiração:** o resultado de cada busca fica no Room por **1 hora**. Dentro desse prazo
  a API não é chamada; depois (ou em “Atualizar”/“Tentar de novo”) busca de novo. Se a rede falhar,
  usa o cache antigo — *offline-first*.
- **Coroutines e cancelamento:** as operações rodam no `viewModelScope` (canceladas quando a tela sai
  da pilha). Uma nova busca cancela a anterior (`Job.cancel()`), e `CancellationException` nunca é
  engolida nos `try/catch`.

### Estrutura de pastas
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
    ├── common/     componentes reutilizáveis (BookListItem, RatingBar, estados de tela…)
    ├── search/ detail/ editor/ shelf/   uma pasta por tela (Screen + ViewModel)
    └── theme/
```

### Onde está cada requisito da Etapa 2

| Requisito | Onde |
|---|---|
| Navigation 3 (rotas, entries, back stack, argumentos) | `ui/navigation/Routes.kt`, `ui/EstanteApp.kt` (`BookDetailRoute(bookId)`) |
| `LazyColumn` / `LazyRow` | `SearchScreen` (resultados e temas), `ShelfScreen` (lista e filtros) |
| Formulário com validação e feedback | `EntryEditorScreen` + `ShelfEntryValidator` (erro por campo + resumo no topo) |
| `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle` | todos os `*ViewModel.kt` e `*Screen.kt` |
| Carregando / conteúdo / vazio / erro | `SearchResultsState`, `BookDetailUiState`, `ShelfUiState` + `LoadingState`/`EmptyState`/`ErrorState` |
| Repository | `data/BookRepository.kt`, `data/ShelfRepository.kt` |
| Operação assíncrona com coroutines e cancelamento | `SearchViewModel.launchSearch`, `BookDetailViewModel.loadDescription` |
| Persistência local reativa | Room (`Flow` na estante) e DataStore (preferências) |
| Acessibilidade | descrições (`contentDescription`), títulos (`heading`), áreas de toque ≥ 48dp, cores com contraste AA, textos em `sp` e telas roláveis para fonte grande, anúncio de erros (`liveRegion`/`error`) |
| Sem segredos no código | API pública sem chave; `local.properties` e `.env` no `.gitignore` |

Itens opcionais atendidos: mapeamento DTO → domínio, testes automatizados, validação de entradas e
nada sensível em logs, cache com expiração, offline-first, animações implícitas (`AnimatedContent`,
`AnimatedVisibility`, `animateItem`, `animateContentSize`), telas adaptáveis (barra inferior no
celular, trilho lateral em telas largas e largura máxima de conteúdo), injeção manual de
dependências com interfaces e fakes, e compartilhamento (Intent de envio).

---

## Parcial (módulo `views`)

- **Duas telas em XML:** `BookListActivity` (lista em `RecyclerView`) e `BookDetailActivity`
  (detalhes). Usa `LinearLayout`, `FrameLayout`, `ScrollView`, `TextView`, `ImageView`, `Space`…
- **Intent explícita com dados:** a lista abre os detalhes com
  `Intent(context, BookDetailActivity::class.java)` levando o id do livro e o estado de leitura; os
  detalhes devolvem o novo estado pela *Activity Result API*.
- **ViewBinding** em tudo (nenhum `findViewById` no código do app).
- **Interação que atualiza a tela:** “Marcar como lido” e as estrelas de nota; o resumo
  “Você leu X de 8 livros” da lista é atualizado ao voltar.
- **Modelos imutáveis:** `Book`, `ReadingState` e `BookItem` são `data class` com `val`; mudanças
  geram cópias (`copy`). Ano, páginas, sinopse e nota são opcionais (`?`) e têm texto alternativo.
- **Dados simulados:** `data/MockBooks.kt`.
- **Opcionais:** componente XML reutilizável (`view_rating.xml` + `RatingView`), interface em
  `Fragment` com ViewBinding respeitando o ciclo de vida (`BookDetailFragment`).

---

## Testes

`./gradlew test` roda **33 testes** na JVM (os de interface usam Robolectric, sem emulador):

- `ShelfEntryValidatorTest` — regras do formulário;
- `DefaultBookRepositoryTest` — cache, expiração, modo offline e erros (com fakes);
- `MappersTest` — conversão DTO → domínio;
- `EstanteDatabaseTest` — Room de verdade (banco em memória);
- `SearchViewModelTest`, `EntryEditorViewModelTest` — estados, debounce/cancelamento, validação;
- `ComponentsTest` — componentes Compose e estados de erro/vazio;
- `EstanteAppFlowTest` — fluxo completo na interface: buscar → detalhes → formulário → salvar → estante;
- `views/NavigationTest` — Intent explícita e interação da Parcial.

---

## Bibliotecas externas

| Biblioteca | Para quê |
|---|---|
| **Jetpack Compose** (Material 3, ícones, navigation-suite) | Interface declarativa do módulo `app`; a *navigation-suite* troca barra inferior por trilho lateral em telas grandes |
| **Navigation 3** | Navegação entre telas com uma pilha de rotas controlada pelo app |
| **Lifecycle** (ViewModel, runtime-compose, viewmodel-navigation3) | `ViewModel`, `collectAsStateWithLifecycle` e um ViewModel por tela da pilha |
| **Room** | Banco de dados local (cache do catálogo e estante do usuário) |
| **DataStore Preferences** | Preferências simples (última busca, filtro da estante) |
| **Retrofit** + **OkHttp** | Chamadas HTTP à API da Open Library |
| **Kotlinx Serialization** | Converter JSON em classes Kotlin (DTOs) e serializar as rotas |
| **Coil** | Carregar as capas dos livros a partir da internet |
| **KSP** | Gera o código do Room em tempo de compilação |
| **AppCompat, Material Components, RecyclerView, Fragment** | Base das telas em XML do módulo `views` |
| **JUnit, Kotlinx Coroutines Test, Robolectric, Compose UI Test** | Testes automatizados |

Dados de livros e capas: [Open Library](https://openlibrary.org/developers/api) (Internet Archive).
