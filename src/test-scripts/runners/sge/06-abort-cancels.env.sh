# Force the 2nd submit call (stage2.txt) to fail. cgpipe should call qdel on
# the 1st jobid before bailing out.
export CGPIPE_MOCK_FAIL_SUBMIT_AT=2
