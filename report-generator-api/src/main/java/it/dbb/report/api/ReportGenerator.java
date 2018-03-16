package it.dbb.report.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * @author daniele
 */
public interface ReportGenerator {

	void generateReport(InputStream template, OutputStream output, String inputMimeType, String outputMimeType,
			Map<String, Object> context) throws PortalException, IOException;

	void generateReport(long fileEntryId, OutputStream output, String outputMimeType, Map<String, Object> context)
			throws PortalException, IOException;
}