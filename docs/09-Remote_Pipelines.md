# Remote pipelines

*Note: this is an experimental feature.*

Pipeline files don't have to live on the local filesystem. CGPipe accepts an HTTP/HTTPS URL anywhere it accepts a path — both on the command line (`cgpipe https://...` or `cgpipe -f https://...`) and inside `include` statements. The fetched content is parsed exactly like a local file.

For convenience, you can register *named remotes* — short prefixes that expand to a base URL.

## Defining custom named remote sources

Defining a new named remote source can be accomplished by setting a new variable in CGPipe (either from a pipeline or from a global RC script). The
new value should be `cgpipe.remote.$shortname$.baseurl`. The value should be the base-url to use for the resource.  As an example:

	cgpipe.remote.compgen_io.baseurl = "https://raw.githubusercontent.com/compgen-io/cgpipe-pipelines/master/"

You can then load a remote pipeline using the `remote-name:filename` syntax. As an example, to see the help text for the pipeline `compgen_io:pipelines`, you'd be able to run the following:

	cgpipe -h -f compgen_io:pipelines

where the pipeline script itself is loaded from `https://raw.githubusercontent.com/compgen-io/cgpipe-pipelines/master/pipelines`.


## SHA-1 hashes

Remote pipelines can change without notice, so CGPipe gives you a way to pin and verify the exact content you expect. Two read-only variables expose the current file's identity:

| Variable | Meaning |
|----------|---------|
| `cgpipe.current.filename` (alias `cgpipe.sys.curfile`) | Path or URL of the file being parsed |
| `cgpipe.current.hash` (alias `cgpipe.sys.curhash`) | SHA-1 hash of that file's content |

Useful for logging which version of a remote ran, or for cross-checking against a manifest.

If you want CGPipe to **refuse to run** unless the hash matches, append `#<expected-sha1>` to the filename:

    cgpipe -f "compgen_io:dnaseq-bwamem#a1b2c3d4..."

If the fetched content's SHA-1 doesn't match, CGPipe errors out before executing anything.

The same syntax works inside `include`:

    include "shared/defaults.cgp#a1b2c3d4..."

(Quote the filename when using the command-line form — without quotes the shell treats `#` as the start of a comment.)

For version-controlled or audit-sensitive deployments, host pipelines on a server you control and pin every hash. The shorthand `#hash` is the easiest way to bake the expected version into the invocation.

