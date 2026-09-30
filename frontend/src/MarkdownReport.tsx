import ReactMarkdown from 'react-markdown';
import './MarkdownReport.css';

type MarkdownReportProps = {
  content: string;
};

export function MarkdownReport({ content }: MarkdownReportProps) {
  return (
    <div className="report markdown-report">
      <ReactMarkdown>{content}</ReactMarkdown>
    </div>
  );
}
