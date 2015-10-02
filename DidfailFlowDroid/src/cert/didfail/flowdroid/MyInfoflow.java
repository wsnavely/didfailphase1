package cert.didfail.flowdroid;

import java.util.Collection;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.options.Options;

public class MyInfoflow extends Infoflow {
	public MyInfoflow() {
		super();
	}

	public MyInfoflow(String androidPath, boolean forceAndroidJar) {
		super(androidPath, forceAndroidJar);
	}

	public MyInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory,
			IPathBuilderFactory pathBuilderFactory) {
		super(androidPath, forceAndroidJar, icfgFactory, pathBuilderFactory);
	}

	@Override
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes) {
		super.initializeSoot(appPath, libPath, classes);
		Options.v().set_output_format(Options.output_format_dex);
	}
}
