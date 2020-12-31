- Recognise other extensions not only `.sh` and possibly enable clever shebang detection. This might be done in Java,
if we decide to move it there to offer fine grained cacheability. It should be great to measure the performance of this
task on massive source sets.
- Provide more typical configurations of this kind of tasks such as maxErrors.
- Support other flags from shellcheck itself.
- Allow users to not use Docker and rely on their own installed binary.
