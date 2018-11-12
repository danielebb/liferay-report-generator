package it.dbb.report.internal.context;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.DocumentKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.FileImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldMetadata;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import fr.opensagres.xdocreport.template.formatter.NullImageBehaviour;

public class ReportContext implements Closeable {

	private InputStream originalInputStream;
	private IXDocReport input;
	private IContext context;
	private Options options;
	private boolean report;
	private boolean convert;

	public ReportContext(InputStream inputStream, String mimeTypeIn, String mimeTypeOut, Map<String, Object> context) throws XDocReportException, IOException {

		originalInputStream = inputStream;

		// create context java model
		if (context != null && !context.isEmpty()) {

			// load the zipped xml file (odt, docx, etc...)
			input = getReport(inputStream);
			this.context = input.createContext(context);
			this.report = true;

			injectImages(StringPool.BLANK, context);

		} else {

			this.report = false;
		}

		// set the format converter
		// search through known available conversion types from
		for (DocumentKind documentKind : DocumentKind.values()) {

			if (documentKind.getMimeType().equals(mimeTypeIn)) {

				options = Options.getFrom(documentKind);
			}
		}

		if (options == null) {

			options = Options.getFrom(mimeTypeIn);
		}

		if (Validator.isNotNull(mimeTypeOut) && !mimeTypeOut.equalsIgnoreCase(mimeTypeIn)) {

			// search through known available conversion types to
			for (ConverterTypeTo converterTypeTo : ConverterTypeTo.values()) {

				if (converterTypeTo.getMimeType().equals(mimeTypeOut)) {

					options.to(converterTypeTo);
				}
			}

			if (Validator.isNull(options.getTo())) {

				options.to(mimeTypeOut);
			}

			this.convert = true;

		} else {

			this.convert = false;
		}
	}

	public InputStream getOriginalInputStream() {

		return originalInputStream;
	}

	public IXDocReport getInput() {

		return input;
	}

	public IContext getContext() {

		return context;
	}

	public Options getOptions() {

		return options;
	}

	public boolean isReport() {

		return report;
	}

	public boolean isConvert() {

		return convert;
	}

	@Override
	public void close() throws IOException {

		StreamUtil.cleanUp(true, originalInputStream);
	}

	protected IXDocReport getReport(InputStream inputStream) throws XDocReportException, IOException {

		return XDocReportRegistry.getRegistry().loadReport(inputStream, TemplateEngineKind.Freemarker);
	}

	protected void injectImages(String fieldNamePrefix, Map context) {

		context.replaceAll((key, value) -> injectImage(fieldNamePrefix, key, value));
	}

	protected void injectImages(String fieldNamePrefix, String key, List list) {

		FieldsMetadata metadata = getInputFieldsMetadata();
		metadata.addFieldAsList(appendFieldName(fieldNamePrefix, key));

		list.replaceAll(valueElement -> injectImage(StringPool.BLANK, key.concat("Item"), valueElement));
	}

	protected Object injectImage(String fieldNamePrefix, Object key, Object value) {

		if (value instanceof InputStream) {

			String fieldName = appendFieldName(fieldNamePrefix, key.toString());

			_log.info("Replacing object " + key + " with a " + ByteArrayImageProvider.class.getName());

			try {

				FieldsMetadata metadata = getInputFieldsMetadata();
				metadata.addFieldAsImage(key.toString(), fieldName, NullImageBehaviour.RemoveImageTemplate);

				return new ByteArrayImageProvider((InputStream) value);

			} catch (IOException e) {

				_log.error(e);
			}

		} else if (value instanceof File) {

			String fieldName = appendFieldName(fieldNamePrefix, key.toString());

			_log.info("Replacing object " + key + " with a " + FileImageProvider.class.getName());

			FieldsMetadata metadata = getInputFieldsMetadata();
			metadata.addFieldAsImage(key.toString(), fieldName, NullImageBehaviour.RemoveImageTemplate);

			return new FileImageProvider((File) value);

		} else if (value instanceof Map) {

			injectImages(appendFieldName(fieldNamePrefix, key.toString()), (Map) value);

		} else if (value instanceof List) {

			injectImages(fieldNamePrefix, key.toString(), (List) value);
		}

		return value;
	}

	protected FieldsMetadata getInputFieldsMetadata() {

		FieldsMetadata fieldsMetadata = input.getFieldsMetadata();

		if (fieldsMetadata == null) {

			fieldsMetadata = new FieldsMetadata();
			input.setFieldsMetadata(fieldsMetadata);
		}

		return fieldsMetadata;
	}

	protected String appendFieldName(String fieldNamePrefix, String fieldName) {

		if (Validator.isNotNull(fieldNamePrefix)) {

			return fieldNamePrefix.concat(StringPool.PERIOD).concat(fieldName);
		}

		return fieldName;
	}

	private static final Log _log = LogFactoryUtil.getLog(ReportContext.class);
}
