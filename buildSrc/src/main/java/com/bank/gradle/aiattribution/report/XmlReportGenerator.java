package com.bank.gradle.aiattribution.report;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Generates XML format attribution report.
 * Suitable for integration with CI/CD tools and enterprise reporting systems.
 */
public class XmlReportGenerator implements ReportGenerator {

    @Override
    public void generate(AttributionReport report, File outputFile) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            
            XMLStreamWriter xml = factory.createXMLStreamWriter(osw);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            
            // Root element
            xml.writeStartElement("aiAttributionReport");
            xml.writeAttribute("version", "1.0");
            xml.writeCharacters("\n");
            
            // Metadata
            writeMetadata(xml, report);
            
            // Summary
            writeSummary(xml, report);
            
            // Tool breakdown
            writeToolBreakdown(xml, report);
            
            // Module breakdown
            writeModuleBreakdown(xml, report);
            
            // Commits
            writeCommits(xml, report);
            
            xml.writeEndElement(); // aiAttributionReport
            xml.writeEndDocument();
            xml.flush();
            xml.close();
            
        } catch (XMLStreamException e) {
            throw new IOException("Failed to generate XML report", e);
        }
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    private void writeMetadata(XMLStreamWriter xml, AttributionReport report) throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("metadata");
        xml.writeCharacters("\n");
        
        writeElement(xml, "projectName", report.getProjectName(), 4);
        writeElement(xml, "projectVersion", report.getProjectVersion(), 4);
        writeElement(xml, "branch", report.getBranch(), 4);
        writeElement(xml, "headCommit", report.getHeadCommit(), 4);
        writeElement(xml, "generatedAt", report.getGeneratedAt().toString(), 4);
        writeElement(xml, "analyzedRange", report.getAnalyzedRange(), 4);
        
        xml.writeCharacters("  ");
        xml.writeEndElement(); // metadata
        xml.writeCharacters("\n");
    }

    private void writeSummary(XMLStreamWriter xml, AttributionReport report) throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("summary");
        xml.writeCharacters("\n");
        
        writeElement(xml, "totalCommits", String.valueOf(report.getTotalCommits()), 4);
        writeElement(xml, "aiAssistedCommits", String.valueOf(report.getAiAssistedCommits()), 4);
        writeElement(xml, "aiAssistedPercentage", String.format("%.2f", report.getAiAssistedPercentage()), 4);
        writeElement(xml, "totalFilesChanged", String.valueOf(report.getTotalFilesChanged()), 4);
        writeElement(xml, "aiAssistedFilesChanged", String.valueOf(report.getAiAssistedFilesChanged()), 4);
        
        xml.writeCharacters("  ");
        xml.writeEndElement(); // summary
        xml.writeCharacters("\n");
    }

    private void writeToolBreakdown(XMLStreamWriter xml, AttributionReport report) throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("toolBreakdown");
        xml.writeCharacters("\n");
        
        for (Map.Entry<AiTool, Integer> entry : report.getCommitsByTool().entrySet()) {
            xml.writeCharacters("    ");
            xml.writeStartElement("tool");
            xml.writeAttribute("name", entry.getKey().getDisplayName());
            xml.writeAttribute("id", entry.getKey().getTrailerId());
            xml.writeAttribute("commits", String.valueOf(entry.getValue()));
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }
        
        xml.writeCharacters("  ");
        xml.writeEndElement(); // toolBreakdown
        xml.writeCharacters("\n");
    }

    private void writeModuleBreakdown(XMLStreamWriter xml, AttributionReport report) throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("moduleBreakdown");
        xml.writeCharacters("\n");
        
        for (Map.Entry<String, AttributionReport.ModuleStats> entry : report.getModuleBreakdown().entrySet()) {
            AttributionReport.ModuleStats stats = entry.getValue();
            xml.writeCharacters("    ");
            xml.writeStartElement("module");
            xml.writeAttribute("name", entry.getKey());
            xml.writeAttribute("totalFiles", String.valueOf(stats.getTotalFiles()));
            xml.writeAttribute("aiAssistedFiles", String.valueOf(stats.getAiAssistedFiles()));
            xml.writeAttribute("aiAssistedPercentage", String.format("%.2f", stats.getAiAssistedPercentage()));
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }
        
        xml.writeCharacters("  ");
        xml.writeEndElement(); // moduleBreakdown
        xml.writeCharacters("\n");
    }

    private void writeCommits(XMLStreamWriter xml, AttributionReport report) throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("commits");
        xml.writeAttribute("count", String.valueOf(report.getTotalCommits()));
        xml.writeCharacters("\n");
        
        for (CommitAttribution commit : report.getCommits()) {
            writeCommit(xml, commit);
        }
        
        xml.writeCharacters("  ");
        xml.writeEndElement(); // commits
        xml.writeCharacters("\n");
    }

    private void writeCommit(XMLStreamWriter xml, CommitAttribution commit) throws XMLStreamException {
        xml.writeCharacters("    ");
        xml.writeStartElement("commit");
        xml.writeAttribute("hash", commit.getShortHash());
        xml.writeAttribute("aiAssisted", String.valueOf(commit.isAiAssisted()));
        xml.writeCharacters("\n");
        
        writeElement(xml, "fullHash", commit.getCommitHash(), 6);
        writeElement(xml, "author", commit.getAuthor(), 6);
        writeElement(xml, "authorEmail", commit.getAuthorEmail(), 6);
        writeElement(xml, "commitTime", commit.getCommitTime().toString(), 6);
        writeElement(xml, "message", commit.getMessage(), 6);
        
        if (commit.isAiAssisted()) {
            writeElement(xml, "aiTool", commit.getAiTool().getDisplayName(), 6);
            if (commit.getAiConfidence() != null) {
                writeElement(xml, "aiConfidence", commit.getAiConfidence(), 6);
            }
        }
        
        // Files changed
        xml.writeCharacters("      ");
        xml.writeStartElement("filesChanged");
        xml.writeAttribute("count", String.valueOf(commit.getFileCount()));
        xml.writeCharacters("\n");
        
        for (String file : commit.getFilesChanged()) {
            xml.writeCharacters("        ");
            xml.writeStartElement("file");
            xml.writeCharacters(file);
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }
        
        xml.writeCharacters("      ");
        xml.writeEndElement(); // filesChanged
        xml.writeCharacters("\n");
        
        xml.writeCharacters("    ");
        xml.writeEndElement(); // commit
        xml.writeCharacters("\n");
    }

    private void writeElement(XMLStreamWriter xml, String name, String value, int indent) throws XMLStreamException {
        for (int i = 0; i < indent; i++) xml.writeCharacters(" ");
        xml.writeStartElement(name);
        if (value != null) {
            xml.writeCharacters(value);
        }
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }
}
