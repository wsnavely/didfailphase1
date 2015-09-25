import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.Result;
import edu.psu.cse.siis.coal.Results;
import edu.psu.cse.siis.coal.field.values.FieldValue;
import edu.psu.cse.siis.coal.values.PathValue;
import edu.psu.cse.siis.coal.values.PropagationValue;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class Main {

	static class IC3Args {
		@Parameter
		private List<String> parameters = new ArrayList<String>();

		@Parameter(names = "-apk", required = true)
		private String apk;

		@Parameter(names = "-androidjar", required = true)
		private String androidJar;

		@Parameter(names = "-retargetedApp", required = true)
		private String retargetedApp;

		@Parameter(names = "-out")
		private String out = null;
	}

	public static void main(String[] args) {

		IC3Args jct = new IC3Args();
		new JCommander(jct, args);

		File output = null;
		if (jct.out != null) {
			output = new File(jct.out);
		}

		IC3Wrapper wrapper = new IC3Wrapper();
		wrapper.setRetargetedPath(jct.retargetedApp);
		wrapper.setApkPath(jct.apk);
		wrapper.setAndroidJarPath(jct.androidJar);
		wrapper.runAnalysis();
		handleResults(output);
	}

	public static void handleResults(File output) {
		Element root = new Element("results");
		for (Result r : Results.getResults()) {
			Map<Unit, Map<Integer, Object>> result = r.getResults();
			for (Map.Entry<Unit, Map<Integer, Object>> entry : result.entrySet()) {
				Unit unit = entry.getKey();
				SootMethod method = AnalysisParameters.v().getIcfg().getMethodOf(unit);
				SootClass declaringClass = method.getDeclaringClass();

				Element icc = new Element("ic");
				icc.addAttribute(new Attribute("unit", unit.toString()));
				icc.addAttribute(new Attribute("method", method.getSubSignature()));
				icc.addAttribute(new Attribute("class", declaringClass.getName()));

				for (Map.Entry<Integer, Object> entry2 : entry.getValue().entrySet()) {
					PropagationValue pv = (PropagationValue) entry2.getValue();
					for (PathValue pathValue : pv.getPathValues()) {
						Map<String, FieldValue> fieldMap = pathValue.getFieldMap();
						for (String field : fieldMap.keySet()) {
							Element fieldElem = new Element("field");
							FieldValue fieldValue = fieldMap.get(field);
							Object value = fieldValue.getValue();
							fieldElem.addAttribute(new Attribute("name", field));
							if (value != null) {
								if (value instanceof Iterable) {
									@SuppressWarnings("rawtypes")
									Iterable iter = (Iterable) value;
									for (Object v : iter) {
										Element valueElem = new Element("value");
										valueElem.appendChild(v + "");
										fieldElem.appendChild(valueElem);
									}
								} else {
									Element valueElem = new Element("value");
									valueElem.appendChild(value + "");
									fieldElem.appendChild(valueElem);
								}
								icc.appendChild(fieldElem);
							}
						}
					}
				}
				root.appendChild(icc);
			}
		}

		Document doc = new Document(root);
		OutputStream os = null;

		try {
			if (output != null) {
				os = new FileOutputStream(output);
			} else {
				os = System.out;
			}
			Serializer serializer = new Serializer(os, "UTF-8");
			serializer.setIndent(4);
			serializer.setMaxLength(64);
			serializer.write(doc);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (output != null && os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
