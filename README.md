# OPSIT DSL
## _The Best DSL, Ever_


### local cloud build
follow the instructions here to enable the builder api 
https://cloud.google.com/build/docs/build-debug-locally?authuser=6

```sh
cloud-build-local -config=cloudbuild.yaml --dryrun=false .
```

### Notes

if you don't update the version of the pom.xml in both projects you will fail!
the build itself perform a `mvn deploy` to gcp artifact registry
hence you'll get a `generic::already_exists: file already exists:`

### Notes

Go to [GCP CLOUD BUILD](https://console.cloud.google.com/cloud-build/builds?authuser=6&project=opsit-compute)
