
# In-Memory Document Database Demo

A demonstration of in-memory document database, powered by [cats-effect](https://typelevel.org/cats-effect/) and [scala 3](https://docs.scala-lang.org/scala3/).

## Build Requirements.

* Scala 3 compiler.
* Java Virtual Machine, version 11 or greater.
* Scala Build Tool `sbt`

## Running

The repository provides sample json files for the type `User` and `Ticket` in the directory `./sample-data/`. 

To view supported command line options, you can run:
```shell
sbt "run --help"
```

You can launch the demo app using the following sbt command.

```shell
sbt "run --users sample-data/users.json --tickets sample-data/tickets.json"
```

As outlined in the specs, once run you are presented with a `Console` which allows you to search tickets or users.

## Test

Test can be run with the command: `sbt test`

## Adding new schema for type for indexing.

To add in-memory supported for any type you would need to provide `DocumentSchema[T, K]` where `T` is the type of the object being stored and `K` refers to its primary key. For the ticket example, the schema is declared as the following:

```scala
  given DocumentSchema[Ticket, TicketId] with
    def name    = "Ticket"
    def primary = IndexField("_id", _.id)
    def nonPrimary = List(
      IndexField("created_at", _.createdAt),
      IndexField("type", _.ticketType),
      IndexField("subject", _.subject),
      IndexField("assignee_id", _.assigneeId),
      IndexField("tags", _.tags)
    )
```

To query a field. You will have to implicitly pass the `DocumentSchema` along with specifying the type parameters when querying with the `Database` trait, so it is able to match the correct documents. Not doing so will result in compiler error.

## Workflow

### Insertion

              ┌───────────────────────────────────────┐
              │                                       │
              │               Insertion               │
              │                                       │
              │ ┌───────────────────────────────────┐ │
              │ │ Encode fields to Index Primitives │ │
              │ └─────────────────┬─────────────────┘ │
              │                   │                   │
              │                   │                   │
              │ ┌─────────────────▼─────────────────┐ │
              │ │   Create IndexData and Document   │ │
              │ └─────────────────┬─────────────────┘ │
              │                   │                   │
              │                   │                   │
              │ ┌─────────────────▼─────────────────┐ │
              │ │   Merge Documents in the database │ │
              │ └───────────────────────────────────┘ │
              │                                       │
              └───────────────────────────────────────┘

### Searching


               ┌────────────────────────────────────┐
               │                                    │
               │              Search                │
               │                                    │
               │ ┌────────────────────────────────┐ │
               │ │Decode input to Index Primitive │ │
               │ └───────────────┬────────────────┘ │
               │                 │                  │
               │                 │                  │
               │ ┌───────────────▼────────────────┐ │
               │ │ Retrieve Documents for schema  │ │
               │ └───────────────┬────────────────┘ │
               │                 │                  │
               │                 │                  │
               │ ┌───────────────▼────────────────┐ │
               │ │  Full match with Index Data    │ │
               │ └────────────────────────────────┘ │
               │                                    │
               └────────────────────────────────────┘
 


## Notes/Assumptions

* Only full value matching is supported when searching via fields. However, for arrays it supports matching any of the array elements.
* There is currently no way to query optional fields whose values are not present.
* Only immutable constructs i.e `case classes` must be used for runtime safety.
* Currently updating the same document already present in the database (identified by `_id`) is unsupported due to stale indexes not being deleted. This makes it best suited for append-only data. 

## Future improvements

* Type-specific indexers. All indexes fields must be converted to internal database representation `IndexValue`. But this is currently under utilized and only supports full value matches. 
For example, it should be possible to add Geospatial indexing, Trie-based indexing etc.
* Macro to derive the schema automatically from case classes.

## Demo 

[![asciicast](https://asciinema.org/a/m27QSxw5GC1EYCRep4c59Fn56.svg)](https://asciinema.org/a/m27QSxw5GC1EYCRep4c59Fn56)
