package uk.ac.ed.epcc.ctp_anon_cli;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    Options options = new Options();

    Option option = Option
      .builder("a")
      .argName("file")
      .hasArg()
      .longOpt("anon-script")
      .desc("Anonymisation script")
      .required()
      .build();
    options.addOption(option);

    CommandLineParser parser = new DefaultParser();
    CommandLine cli = null;
    try {
      cli = parser.parse(options, args);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("ctp-anon-cli.jar", options);
      System.exit(1);
    }

    File anonScriptFile = new File(
      Paths.get(cli.getOptionValue("a")).toString()
    );

    if (!anonScriptFile.isFile()) throw new IllegalArgumentException(
      "Cannot find anonymisation script file: " + anonScriptFile.getPath()
    );

    List<String> files = cli.getArgList();

    if (files.size() == 0) {
      System.out.println("Usage:");
      System.out.println(
        "  Anonymise a single file: ctp-anon-cli.jar -a anon-script <src-file> <anon-file>"
      );
      System.out.println(
        "  Anonymise multiple files: ctp-anon-cli.jar -a anon-script <src-file:anon-file>..."
      );
      System.exit(1);
    }

    // TODO SRAnon
    SmiCtpProcessor anonymizer = new DicomAnonymizerToolBuilder()
      .tagAnonScriptFile(anonScriptFile)
      .check(null)
      // .SRAnonTool(SRAnonTool)
      .buildDat();

    if (
      files.size() == 2 &&
      !files.get(0).contains(":") &&
      !files.get(1).contains(":")
    ) {
      File inFile = new File(files.get(0));
      File outFile = new File(files.get(1));

      ValidateFilePair(inFile, outFile);

      anonymizer.anonymize(inFile, outFile);

      System.exit(0);
    }

    List<File> filePairsToProcess = new ArrayList<File>();

    for (String filePair : files) {
      String[] filePairSplit = filePair.split(":");

      if (filePairSplit.length != 2) {
        System.err.println("Expected two file paths separated by one ':'");
        System.exit(1);
      }

      File inFile = new File(filePairSplit[0]);
      File outFile = new File(filePairSplit[1]);

      ValidateFilePair(inFile, outFile);

      if (
        filePairsToProcess.contains(inFile) ||
        filePairsToProcess.contains(outFile)
      ) {
        System.err.println(
          "Duplicate file pairs passed: '" + inFile + "' or '" + outFile + "'"
        );
        System.exit(1);
      }

      filePairsToProcess.add(inFile);
      filePairsToProcess.add(outFile);
    }

    for (int i = 0; i < filePairsToProcess.size(); i += 2) {
      System.out.println("In:  " + filePairsToProcess.get(i));
      System.out.println("Out: " + filePairsToProcess.get(i + 1));
      anonymizer.anonymize(
        filePairsToProcess.get(i),
        filePairsToProcess.get(i + 1)
      );
    }
  }

  private static void ValidateFilePair(File inFile, File outFile) {
    if (outFile.equals(inFile)) {
      System.err.println(
        "in-file and out-file must be different: '" + inFile + "'"
      );
      System.exit(1);
    }

    if (!inFile.isFile()) {
      System.err.println("in-file does not exist: '" + inFile + "'");
      System.exit(1);
    }

    if (outFile.isFile()) {
      System.err.println("out-file already exists: '" + outFile + "'");
      System.exit(1);
    }
  }
}
