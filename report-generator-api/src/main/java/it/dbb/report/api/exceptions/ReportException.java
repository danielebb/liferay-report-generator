package it.dbb.report.api.exceptions;

import com.liferay.portal.kernel.exception.PortalException;

public class ReportException extends PortalException {

	public ReportException() {
		super();
	}

	public ReportException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public ReportException(String msg) {
		super(msg);
	}

	public ReportException(Throwable cause) {
		super(cause);
	}

}
