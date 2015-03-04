package org.cooperari.tools.csetup;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.cooperari.CBaseConfiguration;
import org.cooperari.CInstrument;
import org.cooperari.core.aspectj.Compiler;
import org.cooperari.core.aspectj.WeavingConfiguration;

/**
 * Main class for the <code>csetup</code> utility program.
 * 
 * @since 0.2
 */
public final class Main {

  /**
   * Program entry point. 
   * 
   * @param args Arguments.
   */
  public static void main(String[] args) {
    int exitCode;
    try {
      exitCode = execute(args, System.out);
    } catch (Throwable e) {
      e.printStackTrace();
      System.out.println("Unexpected error.");
      exitCode = 2;
    }
    System.exit(exitCode);
  }

  /**
   * Execution delegate. 
   * 
   * It behaves like <code>main</code> but can be invoked programmatically.
   * 
   * @param args Program arguments.
   * @param out Output stream.
   * @return A suggested program exit code, <code>0</code> for normal termination,
   *         non-zero in the case of errors.
   */
  public static int execute(String[] args, PrintStream out) {
    boolean dryRun = false;
    boolean quiet = false;
    boolean verbose = false;
    boolean keepTempFiles = false;
    boolean forceDump = false;
    File outDir = new File(System.getProperty("user.home") + "/.cooperari");
    int index = 0;
    if (args.length == 0) {
      out.println("No arguments. Use '-h' for help.");
      return 1;
    }
    while (index < args.length && args[index].charAt(0) == '-') {
      String arg = args[index++];
      if (arg.length() == 0) {
        out.printf("Invalid argument '%s'.\n", arg);
      }
      switch (arg.substring(1)) {
        case "h":
        case "help":
          displayUsage(out);
          return 0;
        case "d":
        case "dir":
          if (index < args.length) {
            outDir = new File(args[index++]);
          } else {
            out.printf("Output directory not specified for '%s'.\n", arg);
            return 1;
          }
          break;
        case "n":
        case "dryrun":
          dryRun = true;
          break;
        case "q":
        case "quiet":
          quiet = true;
          verbose = false;
          break;
        case "v":
        case "verbose":
          quiet = false;
          verbose = true;
          break;
        case "Xd":
          forceDump = true;
          break;
        case "Xk":
          keepTempFiles = true;
          break;
        default:
          out.printf("Invalid argument '%s'.\n", arg);
          return 1;
      }
    }
    if (verbose) {
      out.println("- Processed program arguments ... ");
    }
    try {
      outDir.mkdirs();
    } catch (Throwable e) {
      e.printStackTrace(out);
      out.printf("Error creating output directory '%s'.\n",
          outDir.getAbsolutePath());
      return 1;
    }

    while (index < args.length) {
      String className = args[index++];
      if (verbose) {
        out.printf("Handling class '%s' ...\n", className);
      }
      try {
        Class<?> clazz = Class.forName(className);
        CInstrument config = clazz.getAnnotation(CInstrument.class);
        if (config == null) {
          config = CBaseConfiguration.class.getAnnotation(CInstrument.class);
        }
        
        WeavingConfiguration wc = WeavingConfiguration.create(config, clazz);
        if (verbose) {
          out.println("Created weaving configuration ...");
        }
        if (forceDump) {
          wc.enableDumpForAllClasses();
          wc.addWeaverOption("-verbose");
          wc.addWeaverOption("-showWeaveInfo");
          wc.addWeaverOption("-debug");
        }
        try {
          if (dryRun || verbose) {
            out.printf("== %s :: XML dump ==\n", className);
            wc.print(out);
          } 
          if (!dryRun) {
            File abcInpFile = new File(outDir, className
                + "_uncompiled_aspects.jar");
            File abcOutFile = new File(outDir, className
                + "_compiled_aspects.jar");
            if (!keepTempFiles) {
              abcInpFile.deleteOnExit();
              abcOutFile.deleteOnExit();
            }
            ArrayList<String> ajcMsgs = new ArrayList<>();
            if (verbose) {
              out.println("Calling AspectJ compiler ...");
            }
            boolean cResult = Compiler.compile(wc.getAspectClasses(),
                verbose ? "-verbose" : "", abcInpFile, abcOutFile, ajcMsgs);
            
            if (!cResult || verbose) {
              for (String msg : ajcMsgs) {
                out.print(msg);
              }
            } 
            
            if (!cResult) {
              out.println("There were errors during AspectJ compilation!");
              return 1;
            }
          
            if (verbose) {
              out.printf("%s - %d bytes\n", abcInpFile.getCanonicalPath(), abcInpFile.length());
              out.printf("%s - %d bytes\n", abcOutFile.getCanonicalPath(), abcOutFile.length());
              out.println("Saving to final JAR file ...");
            }
            
            File jarFile = new File(outDir, className + "-cooperari.jar");
            wc.saveAsJAR(abcOutFile, jarFile);
            if (!quiet) {
              out.printf("JAR file for '%s' saved to '%s'\n", className,
                  jarFile);
            }
          }
        } catch (IOException e) {
          e.printStackTrace(out);
          out.printf("I/O error saving configuration for class '%s'.",
              className);
          return 1;
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace(out);
        out.printf("'%s' not found on class-path.\n", className);
        return 1;
      }
    }
    return 0;
  }

  /**
   * Display usage.
   * 
   * @param out Output stream.
   */
  private static void displayUsage(PrintStream out) {
    out.println("Usage:\n  java " + Main.class.getCanonicalName()
        + " [JVM options] [program options] class1 ... classn");
    out.println("Program options:\n"
        + "  -d <dir> | -dir <dir> : set output directory\n"
        + "  -h | -help            : display this message and exit\n"
        + "  -n | -dryrun          : dry run (just dump XML for weaving configurations)\n"
        + "  -q | -quiet           : run silent (no output)\n"
        + "  -v | -verbose         : be verbose (extra output)\n"
        + "  -Xd                   : force class dump during load-time weaving\n"
        + "  -Xk                   : keep intermediate temporary files\n"
        );

  }
  /**
   * Private constructor to prevent instantiation.
   */
  private Main() {
    
  }

}
