# How to release

A release can be made by creating a tag with the pattern
`release/VERSION`. This will trigger a github action that will publish the
jar file to the github maven repository, with version `VERSION`.

For more information checkout the following files

- [build.gradle.kts](build.gradle.kts)
- [on-tag-publish.yml](.github/workflows/on-tag-publish.yml)
