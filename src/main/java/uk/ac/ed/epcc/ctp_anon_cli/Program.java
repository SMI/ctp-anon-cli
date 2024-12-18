package uk.ac.ed.epcc.ctp_anon_cli;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
      .builder("v")
      .longOpt("version")
      .desc("Print version info and exit")
      .build();
    options.addOption(option);

    CommandLineParser parser = new DefaultParser();
    CommandLine cli = null;

    try {
      cli = parser.parse(options, args, true);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("ctp-anon-cli.jar", AddRequiredOptions(options));
      System.exit(1);
    }

    if (cli.hasOption("v")) {
      System.out.println(
        "ctp-anon-cli: " + Program.class.getPackage().getImplementationVersion()
      );
      System.exit(0);
    }

    options = AddRequiredOptions(options);

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
      "Cannot find anonymisation script file"
    );

    File srAnonTool = null;
    String srAnonToolPath = cli.getOptionValue("s");
    if (!srAnonToolPath.toLowerCase().equals("false")) {
      srAnonTool = new File(Paths.get(srAnonToolPath).toString());
      if (!srAnonTool.isFile()) {
        throw new IllegalArgumentException("Cannot find SRAnonTool");
      }
    }

    List<String> files = cli.getArgList();
    boolean runAsDaemon = false;

    if (files.size() == 0) {
      if (cli.hasOption("d")) {
        runAsDaemon = true;
      } else {
        System.out.println("Usage:");
        System.out.println(
          "  Anonymise a single file: ctp-anon-cli.jar -a anon-script <src-file> <anon-file>"
        );
        System.out.println(
          "  Anonymise multiple files: ctp-anon-cli.jar -a anon-script <src-file:anon-file>..."
        );
        System.exit(1);
      }
    }

    SmiCtpProcessor anonymizer = new SmiCtpProcessor(
      anonScriptFile,
      null,
      false,
      false,
      false,
      false,
      null,
      srAnonTool
    );

    if (runAsDaemon) {
      System.out.println("READY");
      try (Scanner scanner = new Scanner(System.in)) {
        while (true) {
          String line = scanner.nextLine();
          String[] filePairSplit = line.split(" ");

          if (filePairSplit.length != 2) {
            System.out.println("Expected two file paths");
            continue;
          }

          File inFile = new File(filePairSplit[0]);
          File outFile = new File(filePairSplit[1]);

          try {

            outFile = ValidateFilePair(inFile, outFile);

            anonymizer.anonymize(inFile, outFile);
          } catch (Exception e) {
            System.out.println("[" + inFile + ":" + outFile + "] " + e.getMessage());
            continue;
          }
        }
      }
    }

    if (
      files.size() == 2 &&
      !files.get(0).contains(":") &&
      !files.get(1).contains(":")
    ) {
      File inFile = new File(files.get(0));
      File outFile = new File(files.get(1));

      try {
        outFile = ValidateFilePair(inFile, outFile);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }

      try {
          anonymizer.anonymize(inFile, outFile);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }

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

      try {
        outFile = ValidateFilePair(inFile, outFile);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }

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
      try {
        anonymizer.anonymize(
          filePairsToProcess.get(i),
          filePairsToProcess.get(i + 1)
        );
      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }

    System.exit(0);
  }

  private static Options AddRequiredOptions(Options options) {
    Option option = Option
      .builder("d")
      .longOpt("daemonize")
      .desc("Run as a daemon and wait for files to process")
      .build();
    options.addOption(option);

    option =
      Option
        .builder("a")
        .argName("file")
        .hasArg()
        .longOpt("anon-script")
        .desc("Anonymisation script")
        .required()
        .build();
    options.addOption(option);

    option =
      Option
        .builder("s")
        .argName("file")
        .hasArg()
        .longOpt("sr-anon-tool")
        .desc("SR anonymisation tool")
        .required()
        .build();
    options.addOption(option);

    return options;
  }

  private static File ValidateFilePair(File inFile, File outFile)
    throws Exception {
    if (outFile.equals(inFile)) {
      throw new Exception(
        "in-file and out-file must be different: '" + inFile + "'"
      );
    }

    if (!inFile.isFile()) {
      throw new Exception("in-file does not exist: '" + inFile + "'");
    }

    if (outFile.isFile()) {
      throw new Exception("out-file already exists: '" + outFile + "'");
    }

    // NOTE: DICOMAnonymizer doesn't handle outFiles without a prefix and attempts to
    // write to "/" on Unix systems
    if (outFile.getParent() == null) {
      outFile = new File(outFile.getAbsolutePath());
    }

    return outFile;
  }
}
