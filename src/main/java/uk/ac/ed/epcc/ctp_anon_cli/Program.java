package uk.ac.ed.epcc.ctp_anon_cli;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Program {

  public static void main(String[] args) throws ParseException {
    CommandLine cli = ParseCliOptions(args);

    if (cli == null) System.exit(1);

    File anonScriptFile = new File(
      Paths.get(cli.getOptionValue("a")).toString()
    );

    if (!anonScriptFile.isFile()) throw new IllegalArgumentException(
      "Cannot find anonymisation script file: " + anonScriptFile.getPath()
    );

    List<String> files = cli.getArgList();

    if (files.size() == 0 || files.size() % 2 != 0) {
      // print usage and exit
      System.exit(123);
    }

    // TODO SRAnon
    // SmiCtpProcessor anonTool = new DicomAnonymizerToolBuilder()
    //   .tagAnonScriptFile(anonScriptFile)
    //   .check(null)
    //   //   .SRAnonTool(SRAnonTool)
    //   .buildDat();

    if (
      files.size() == 2 &&
      !files.get(0).contains(":") &&
      !files.get(1).contains(":")
    ) {
      // Anon foo -> foo.anon and exit
      System.exit(123);
    }

    List<String> filePairsToProcess = new ArrayList<String>();

    for (String filePair : files) {
      if (!filePair.contains(":")) {
        // print and exit
        System.exit(123);
      }
      String[] filePairSplit = filePair.split(":");
      if (filePairSplit.length != 2) {
        // print and exit
        System.exit(123);
      }
      filePairsToProcess.addAll(Arrays.asList(filePairSplit));
    }

    for (int i = 0; i < filePairsToProcess.size(); i += 2) {
      System.out.println("In:  " + filePairsToProcess.get(i));
      System.out.println("Out: " + filePairsToProcess.get(i + 1));
    }
  }

  private static CommandLine ParseCliOptions(String[] args) {
    Options options = new Options();

    Option option = Option
      .builder("a")
      .argName("file")
      .hasArg()
      .longOpt("anon")
      .desc("Anonymisation script")
      .required()
      .build();
    options.addOption(option);

    CommandLineParser parser = new DefaultParser();
    CommandLine commandLine = null;
    try {
      commandLine = parser.parse(options, args);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("ctp-anon-cli", options);
    }

    return commandLine;
  }

  private static void Anonymise() {}
}
