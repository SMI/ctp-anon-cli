[![main](https://github.com/smi/ctp-anon-cli/actions/workflows/main.yml/badge.svg)](https://github.com/smi/ctp-anon-cli/actions/workflows/main.yml)
[![pre-commit.ci status](https://results.pre-commit.ci/badge/github/SMI/ctp-anon-cli/main.svg)](https://results.pre-commit.ci/latest/github/SMI/ctp-anon-cli/main)

# CTP Anonymiser CLI

A CLI tool to anonymise files using the RSNA MIRC Clinical Trials Processor
(CTP)
[DICOM Anonymizer](https://mircwiki.rsna.org/index.php?title=The_CTP_DICOM_Anonymizer).
Uses the [ctp-anon-minimal](https://github.com/SMI/ctp-anon-minimal) dependency.

## Usage

Requires a Java JRE `>= 8`. Download a jar from the Releases, then run with
`java -jar <jar>`

Required arguments:

- `-a` / `--anon-script`: Path to a CTP DICOM anonymizer script file. Samples
  can be found
  [here](https://github.com/johnperry/CTP/tree/master/source/files/profiles/dicom)
- `-s` / `--sr-anon-tool`: Path to the
  [SRAnonTool](https://github.com/SMI/SmiServices/tree/master/src/applications/SRAnonTool). Can be set to `false` if SR anonymisation is being performed elsewhere.

Optional arguments:

- `-d` / `--daemonize`: Run as a daemon and wait for files to process

To anonymise a single file:

```console
$ java -jar <jar> -a <anon-script> -s <sr-anon-tool> <src-file> <anon-file>
```

To anonymise multiple files:

```console
$ java -jar <jar> -a <anon-script> -s <sr-anon-tool> (<src-file>:<anon-file>)...
```

To run as a daemon and process many files:

```console
$ java -jar <jar> -a <anon-script> -s <sr-anon-tool> -d
```

The application will then wait for lines of `<src-file> <anon-file>` pairs to
process.

## Development

Requirements:

- Java 8 JDK
- Maven

TODO:

- Github maven repo setup
- ctp-anon-minimal dependencies
