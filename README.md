# DodamDodam Backend

Backend service for the DodamDodam capstone project.

The repository is prepared for Spring Boot. CI automatically becomes active
when a Gradle or Maven wrapper, its build file, and `.java-version` are added.
The project owns its Java version; CI does not hardcode one in advance.

Organization workflow documentation is maintained in the
[integration repository](https://github.com/DodamDodam-Capstone/integration/blob/main/docs/GITHUB_WORKFLOW.md).

Changes are promoted from `development` to `main` through a protected,
human-approved squash pull request. A successful `main` promotion then updates
the immutable backend SHA in the integration repository through a bot PR.
