package org.anarres.cpp;

import de.fosd.typechef.featureexpr.MacroContext;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DebuggingPreprocessor {
    public static Logger logger = Logger.getLogger("de.ovgu.jcpp");
    public static boolean DEBUG_TOKENSTREAM = false;
    static {
        try {
            Handler fh;
            fh = new FileHandler("jcpp.log");
            logger.addHandler(fh);
            logger.setLevel(Level.WARNING);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int max_nesting = 0;
    int header_count = 0;
    Set<String> distinctHeaders = new HashSet<String>();

    BufferedWriter debugFile;
    BufferedWriter debugSourceFile;
    {
        try {
            if (DEBUG_TOKENSTREAM)
                debugFile = new BufferedWriter(new FileWriter(new File(
                        "tokenstream.txt")));
            debugSourceFile = new BufferedWriter(new FileWriter(new File(
                    "debugsource.txt")));
        } catch (IOException e) {
        }
    }

    protected abstract MacroContext getMacros();

    public void debugWriteMacros() {
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(
                    "macroDebug.txt")));
            getMacros().debugPrint(writer);
            writer.close();
            // Confusing - it advances some debug files but not others.
            // debugNextTokens();

            // also add statistics to debugSourceFile
            if (debugSourceFile != null) {
                debugSourceFile
                        .append("\n\n\nStatistics (max_nesting,header_count,distinct files):\n"
                                + max_nesting
                                + ";"
                                + header_count
                                + ";"
                                + distinctHeaders.size() + "\n");
                debugSourceFile.flush();
            }

            logger.info("macro dump written");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract Token parse_main() throws IOException, LexerException;

//	private void debugNextTokens() {
//		for (int i = 0; i < 20; i++)
//			try {
//				parse_main();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (LexerException e) {
//				e.printStackTrace();
//			}
//	}

    public void debug_receivedToken(Source source, Token tok) {
        if (DEBUG_TOKENSTREAM && tok != null)
            try {
                Source tmpSrc = source.getParent();
                while (tmpSrc != null) {
                    debugFile.write("\t");
                    tmpSrc = tmpSrc.getParent();
                }
                if (tok.getText() != null)
                    debugFile.write(tok.getText() + "\n");
                debugFile.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    int debugSourceIdx = 0;

    public void debugSourceBegin(Source source, State state) {
        if (source instanceof FileLexerSource) {
            debugSourceIdx++;
            try {
                StringBuffer b = new StringBuffer();
                max_nesting = Math.max(max_nesting, debugSourceIdx);
                distinctHeaders.add(source.toString());
                header_count++;
                for (int i = 1; i < debugSourceIdx; i++)
                    b.append("\t");
                b
                        .append("push "
                                + source.toString()
                                + " -- "
                                + (state == null ? "null" : state
                                .getLocalFeatureExpr()
                                + " ("
                                + state.getFullPresenceCondition()
                                + ")") + "\n");
//				 System.out.println(b.toString());
                debugSourceFile.write(b.toString());
                debugSourceFile.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void debugSourceEnd(Source source) {
        if (source instanceof FileLexerSource) {
            debugSourceIdx--;
            try {
                StringBuffer b = new StringBuffer();
                for (int i = 0; i < debugSourceIdx; i++)
                    b.append("\t");
                b.append("pop " + source.toString() + "\n");
//				 System.out.println(b.toString());
                debugSourceFile.write(b.toString());
                debugSourceFile.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
