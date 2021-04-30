package com.felipefzdz.gradle.shellcheck

import spock.lang.IgnoreIf

@IgnoreIf({ env['SHELLCHECK_PATH'] == null })
class ShellcheckBinaryPluginFuncTest extends BaseShellcheckPluginFuncTest {
    boolean useDocker = false
    String shellcheckBinary = System.getenv('SHELLCHECK_PATH')
}
