
# Remote pipelines

* Note: this is an experimental feature *


Pipeline files don't have to be on the local filesystem in order to be run. Remote pipelines can be used anywhere that a local pathname could be used for CGPipe. Remote pipelines can either be given as explicit HTTP or HTTPS URLs or use a "named remote". Named remotes are web addresses that can be accessed using a shortcut naming scheme to make it easier to use interactively.

## Defining custom named remote sources

Defining a new named remote source can be accomplished by setting a new variable in CGPipe (either from a pipeline or from a global RC script). The
new value should be `cgpipe.remote.$shortname$.baseurl`. The value should be the base-url to use for the resource.  As an example:

	cgpipe.remote.compgen_io.baseurl = "https://raw.githubusercontent.com/compgen-io/cgpipe-pipelines/master/"

You can then load a remote pipeline using the `remote-name:filename` syntax. As an example, to see the help text for the pipeline `compgen_io:pipelines`, you'd be able to run the following:

	cgpipe -h -f compgen_io:pipelines

where the pipeline script itself is loaded from `https://raw.githubusercontent.com/compgen-io/cgpipe-pipelines/master/pipelines`.


## SHA-1 hashes

Because remote pipelines can be updated at any time, it is important to be able to track if a pipeline is what you expect it to be. This can either mean logging the SHA-1 hash of the script, actively verifying the SHA-1 hash of a local or remote pipeline. The current filename and hash is available with the special variables `cgpipe.current.filename` and `cgpipe.current.hash`.

*Note: If tracking the versions of pipelines is critical (as in version-controlled pipelines), then it is recommended that any remote pipelines be located on a server controlled by you and that each file hash is verified as the pipeline is loaded.*

You can verify any local or remote pipelines by including the expected SHA-1 hash in the filename as shown below.

From within the pipeline:

    `include remote/filename#expected-sha1-hash`

or using the cgpipe executable syntax:

    `cgpipe -f "localfile#sha1-hash"` (Note the quotes - otherwise the `#` would be interpreted as a comment)

