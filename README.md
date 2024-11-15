# NEWM Server
Backend components to support mobile apps and the artist portal. The code is written 100% in [Kotlin](https://kotlinlang.org).

## Setup Environment
### IntelliJ IDEA
[IntelliJ IDEA](https://www.jetbrains.com/idea) is used for development.  

### Ktlint
[Ktlint]("https://ktlint.github.io/") is used in CI to lint the codebase for every PR.
Before opening a PR, it would be beneficial to run Ktlint locally. 

The project contains a pre-commit hook to run Ktlint for every commit. To enable it, 
install Ktlint and then run the following command:

`git config core.hooksPath .githooks`

## Documentation

[REST API Wiki](https://github.com/projectNEWM/newm-server/wiki)

[Ktor Server Documentation](https://ktor.io/docs/ktor-server.html)

## ⚖️ License

```
Copyright 2022-2024 Project NEWM
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
