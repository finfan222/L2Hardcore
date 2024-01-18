package net.sf.l2j.commons.logging.formatter;

import net.sf.l2j.commons.logging.MasterFormatter;

import java.util.logging.LogRecord;

public class GMAuditFormatter extends MasterFormatter {
    @Override
    public String format(LogRecord record) {
        return "[" + getFormatedDate(record.getMillis()) + "]" + SPACE + record.getMessage() + CRLF;
    }
}