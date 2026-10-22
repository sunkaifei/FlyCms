package com.flycms.module.search;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.SegToken;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.List;

/**
 * jieba 中文分词 Lucene 适配器（com.huaban:jieba-analysis，Apache-2.0）。
 *
 * <p>双粒度与 IK 同设计：<b>INDEX 模式</b>细粒度切分（建索引提召回）、
 * <b>SEARCH 模式</b>精确切分（查询提精度）。词干统一小写（拉丁词大小写不敏感匹配）。
 *
 * <p>线程模型：{@link JiebaSegmenter} 词典为静态单例，segmenter 实例在
 * {@link #createComponents} 每线程各建一个，无共享可变状态。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class JiebaAnalyzer extends Analyzer {

    private final SegMode mode;

    public JiebaAnalyzer(SegMode mode) {
        this.mode = mode;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new JiebaTokenizer(mode);
        return new TokenStreamComponents(source, new LowerCaseFilter(source));
    }

    private static final class JiebaTokenizer extends Tokenizer {

        private final JiebaSegmenter segmenter = new JiebaSegmenter();
        private final SegMode mode;
        private final CharTermAttribute term = addAttribute(CharTermAttribute.class);
        private final OffsetAttribute offset = addAttribute(OffsetAttribute.class);

        private List<SegToken> tokens;
        private int pos;
        private int finalEnd;

        private JiebaTokenizer(SegMode mode) {
            this.mode = mode;
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (tokens == null || pos >= tokens.size()) {
                return false;
            }
            SegToken t = tokens.get(pos++);
            clearAttributes();
            term.setEmpty().append(t.word);
            offset.setOffset(correctOffset(t.startOffset), correctOffset(t.endOffset));
            return true;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = input.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            String text = sb.toString();
            finalEnd = text.length();
            tokens = segmenter.process(text, mode);
            // INDEX 模式对长词二次切分时 token 可能乱序，Lucene 要求 offset 单调递增
            if (tokens != null) {
                tokens.sort(java.util.Comparator.comparingInt(t -> t.startOffset));
            }
            pos = 0;
        }

        @Override
        public void end() throws IOException {
            super.end();
            offset.setOffset(correctOffset(finalEnd), correctOffset(finalEnd));
        }
    }
}
