package de.opal.exporter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/*
 * https://stackoverflow.com/questions/29964638/args4j-list-arguments-that-contain-space-not-handled
 */

public class WellBehavedStringArrayOptionHandler extends OptionHandler<String> {
	  public WellBehavedStringArrayOptionHandler(
	      CmdLineParser parser, OptionDef option, Setter<String> setter) {
	    super(parser, option, setter);
	  }

	  @Override
	  public String getDefaultMetaVariable() {
	    return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();            
	  }

	  @Override
	  public int parseArguments(Parameters params) throws CmdLineException {
	    int counter;
	    for (counter = 0; counter < params.size(); counter++) {
	      String param = params.getParameter(counter);
	      if(param.startsWith("-")) {
	        break;
	      }
	      setter.addValue(param);
	    }
	    return counter;
	  }
	}

