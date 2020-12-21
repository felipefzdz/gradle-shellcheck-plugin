#!/usr/bin/env bash

if [[ ! " ${ALLOWED_OLD_VERSIONS[@]} " =~ " ${pg_version} " ]]; then
    K8S_NAMESPACE={{repl if ConfigOptionEquals "openshift_install" "0" }}{{repl ConfigOption "k8s_namespace" }}{{repl end }}
fi
