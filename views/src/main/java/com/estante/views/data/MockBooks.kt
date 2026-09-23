package com.estante.views.data

import com.estante.views.R
import com.estante.views.model.Book

/**
 * Dados simulados (mock) — nesta etapa não há API nem banco de dados.
 * Alguns livros propositalmente não têm ano, páginas ou sinopse,
 * para mostrar o tratamento de valores opcionais na interface.
 */
object MockBooks {

    val all: List<Book> = listOf(
        Book(
            id = 1,
            title = "Código Limpo",
            author = "Robert C. Martin",
            year = 2008,
            pageCount = 425,
            synopsis = "Como escrever código fácil de ler e de manter: bons nomes, funções " +
                "pequenas, comentários na medida certa e testes que dão segurança para mudar.",
            coverColor = R.color.cover_1,
        ),
        Book(
            id = 2,
            title = "O Programador Pragmático",
            author = "Andrew Hunt e David Thomas",
            year = 1999,
            pageCount = 352,
            synopsis = "Dicas práticas para crescer na carreira de desenvolvimento: evitar " +
                "duplicação (DRY), automatizar tarefas e assumir responsabilidade pelo código.",
            coverColor = R.color.cover_2,
        ),
        Book(
            id = 3,
            title = "Entendendo Algoritmos",
            author = "Aditya Y. Bhargava",
            year = 2016,
            pageCount = 264,
            synopsis = "Um guia ilustrado de algoritmos: busca binária, recursão, ordenação, " +
                "grafos e programação dinâmica explicados com desenhos e exemplos simples.",
            coverColor = R.color.cover_3,
        ),
        Book(
            id = 4,
            title = "Refatoração",
            author = "Martin Fowler",
            year = 1999,
            pageCount = 448,
            synopsis = null,
            coverColor = R.color.cover_4,
        ),
        Book(
            id = 5,
            title = "Padrões de Projeto",
            author = "Gamma, Helm, Johnson e Vlissides",
            year = 1994,
            pageCount = null,
            synopsis = "O catálogo clássico de 23 padrões de projeto orientados a objetos, " +
                "como Singleton, Observer, Strategy e Factory Method.",
            coverColor = R.color.cover_5,
        ),
        Book(
            id = 6,
            title = "Arquitetura Limpa",
            author = "Robert C. Martin",
            year = 2017,
            pageCount = 432,
            synopsis = "Como organizar um sistema em camadas independentes, para que regras " +
                "de negócio não dependam de banco de dados, interface ou frameworks.",
            coverColor = R.color.cover_6,
        ),
        Book(
            id = 7,
            title = "Lógica de Programação",
            author = "André Forbellone e Henri Eberspächer",
            year = 1993,
            pageCount = 218,
            synopsis = null,
            coverColor = R.color.cover_1,
        ),
        Book(
            id = 8,
            title = "Apostila de Estruturas de Dados",
            author = "Autor desconhecido",
            year = null,
            pageCount = null,
            synopsis = null,
            coverColor = R.color.cover_2,
        ),
    )

    fun findById(id: Int): Book? = all.firstOrNull { it.id == id }
}
