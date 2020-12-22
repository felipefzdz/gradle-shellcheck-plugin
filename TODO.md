- Improve incrementality of the plugin
- Verify its configuration cache compliance
- Recognise other extensions not only `.sh` and possibly enable clever shebang detection. This might be done in Java,
if we decide to move it there to offer fine grained cacheability. It should be great to measure the performance of this
task on massive source sets.
- Expand, and probably rename, `shellScripts` to a list to enable different folders per submodule, as there's no conventions
on where the shell scripts should be located.
- Provide a build scan snippet on the README on how to publish these violations as custom values.
- Rename errors to violations on the user facing extension.
- Provide more typical configurations of this kind of tasks such as maxErrors.
- Support other flags from shellcheck itself.
- Allow users to not use Docker and rely on their own installed binary.
