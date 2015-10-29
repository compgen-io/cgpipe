
# Remote pipelines

Pipeline files don't have to be on the local filesystem in order to be run. Remote pipelines can be used anywhere that a local pathname could be used. Remote pipelines can either be given as explicit HTTP or HTTPS URLs or use "named remote". Named remotes are remote web sites that can be accessed using a shortcut naming scheme to make it easier to use interactively.

*Note: If you will be using the same pipeline from multiple hosts, it is advisable to store the pipelines on a common web server and setup a named remote.*

## Available compgen.io pipelines

For more information on available remote pipelines from compgen.io, run `cgpipe -h -f compgen_io:pipelines`.

## Defining custom named remote sources

## SHA-1 hashes

Because remote pipelines can be updated at any time, it is important to be able to track if a pipeline is what you expect it to be. This can either mean logging the SHA-1 hash of the script, actively verifying the SHA-1 hash of a local or remote pipeline. The current filename and hash is available with the special variables `cgpipe.current.filename` and `cgpipe.current.hash`.

*Note: If tracking the versions of pipelines is critical (as in version-controlled pipelines), then it is recommended that any remote pipelines be located on a server controlled by you and that each file hash is verified as the pipeline is loaded.*

You can verify any local or remote pipelines by including the expected SHA-1 hash in the filename as shown below.

From within the pipeline:

    `include remote/filename#expected-sha1-hash`

or using the cgpipe executable syntax:

    `cgpipe -f "localfile#sha1-hash"` (Note the quotes - otherwise the `#` would be interpreted as a comment)

