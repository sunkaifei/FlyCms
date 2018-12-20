package com.flycms.module.other.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.core.utils.SymbolConvertUtils;
import com.flycms.module.other.dao.FilterKeywordDao;
import com.flycms.module.other.model.FilterKeyword;
import com.flycms.module.other.model.WordNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 思路： 创建一个FilterSet，枚举了0~65535的所有char是否是某个敏感词开头的状态
 *
 * 判断是否是 敏感词开头 | | 是 不是 获取头节点 OK--下一个字 然后逐级遍历，DFA算法
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Service
public class FilterKeywordService {

	@Autowired
	private FilterKeywordDao filterKeywordDao;

	private static final FilterSet set = new FilterSet(); // 存储首字
	private static final Map<Integer, WordNode> nodes = new HashMap<Integer, WordNode>(1024, 1); // 存储节点
    //大写转化为小写 全角转化为半角，转化的48个干扰符号如下：
    // [!, ., ,, #, $, %, &, *, (, ), |, ?, /, @, ", ', ;, [, ], {, }, +, ~, -, _, =, ^, <, >, 　, ！, 。, ，, ￥, （, ）, ？, 、, “, ‘, ；, 【, 】, ——, ……, 《, 》]
    private static final Integer[] staffs = new Integer[]{64, 12289, 12290, 12298, 12299, 12304, 12305, 8212, 8216, 91, 8220, 93, 94, 95, 32, 33, 34, 35, 36, 37, 65509, 38, 8230, 39, 40, 41, 42, 43, 44, 45, 46, 47, 59, 123, 124, 60, 125, 61, 126, 62, 63};
    private static final Set<Integer> stopwdSet = new HashSet<>(Arrays.asList(staffs));
	private static final char SIGN = '*'; // 敏感词过滤替换
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 添加违禁关键词
     *
     * @param keyword
     *         关键词
     * @return
     */
    @CacheEvict(value = "longterm", allEntries = true)
    public DataVo addFilterKeyword(String keyword) {
        DataVo data = DataVo.failure("操作失败");
        if(this.checkFilterKeyword(keyword)){
            return data=DataVo.failure("该关键词已存在！");
        }
		SnowFlake snowFlake = new SnowFlake(2, 3);
		FilterKeyword keybaen=new FilterKeyword();
		keybaen.setId(snowFlake.nextId());
		keybaen.setKeyword(keyword);
        int totalCount=filterKeywordDao.addFilterKeyword(keybaen);
        if(totalCount > 0){
            data = DataVo.success("添加成功");
        }else{
            data=DataVo.failure("添加失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除违禁关键词
    @CacheEvict(value = "longterm", allEntries = true)
    public DataVo deleteFilterKeywordById(Long id){
        DataVo data = DataVo.failure("操作失败");
        int totalCount=filterKeywordDao.deleteFilterKeywordById(id);
        if(totalCount > 0){
            data = DataVo.success("删除成功");
        }else{
            data=DataVo.failure("删除失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
	/**
	 * 添加违禁关键词
	 *
	 * @param keyword
	 *         关键词
	 * @return
	 */
	@CacheEvict(value = "longterm", allEntries = true)
	public DataVo updateFilterKeywordById(String keyword,Long id) {
		DataVo data = DataVo.failure("操作失败");
		if(this.checkFilterKeywordNotId(keyword,id)){
			return data=DataVo.failure("该关键词已存在！");
		}
		int totalCount=filterKeywordDao.updateFilterKeywordById(keyword,id);
		if(totalCount > 0){
			data = DataVo.success("添加成功");
		}else{
			data=DataVo.failure("添加失败！");
		}
		return data;
	}

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
	/**
	 * 按id查询定时任务配置
	 *
	 * @param id
	 * @return
	 */
	public FilterKeyword findFilterKeywordById(Long id){
		return filterKeywordDao.findFilterKeywordById(id);
	};

    /**
     * 查询违禁关键词是否存在
     *
     * @param keyword
     *         关键词
     * @return
     */
	public boolean checkFilterKeyword(String keyword) {
		int totalCount = filterKeywordDao.checkFilterKeyword(keyword);
		return totalCount > 0 ? true : false;
	}

	/**
	 * 查询当前id以外该违禁关键词是否存在
	 *
	 * @param keyword
	 *         关键词
	 * @param id
	 *         需要排除的id
	 * @return
	 */
	public boolean checkFilterKeywordNotId(String keyword,Long id) {
		int totalCount = filterKeywordDao.checkFilterKeywordNotId(keyword,id);
		return totalCount > 0 ? true : false;
	}

	/**
     *  所有违禁关键词列表
     *
     * @return
     */
    @Cacheable(value = "longterm")
    public List<String> getFilterKeywordAllList(){
        return filterKeywordDao.getFilterKeywordAllList();
    }

    /**
     * 违禁关键词翻页查询
     *
     * @param pageNum
     *         当前页码
     * @param rows
     *         每页数量
     * @return
     */
    public PageVo<FilterKeyword> getFilterKeywordListPage(int pageNum, int rows) {
        PageVo<FilterKeyword> pageVo = new PageVo<FilterKeyword>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(filterKeywordDao.getFilterKeywordList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(filterKeywordDao.getFilterKeywordCount());
        return pageVo;
    }

	/**
	 * 添加DFA节点
	 * 
	 * @param words
	 */
	private void addSensitiveWord(final List<String> words) {
		if (!isEmpty(words)) {
			char[] chs;
			int fchar;
			int lastIndex;
			WordNode fnode; // 首字母节点
			for (String curr : words) {
				chs = curr.toCharArray();
				fchar = charConvert(chs[0]);
				if (!set.contains(fchar)) {// 没有首字定义
					set.add(fchar);// 首字标志位 可重复add,反正判断了，不重复了
					fnode = new WordNode(fchar, chs.length == 1);
					nodes.put(fchar, fnode);
				} else {
					fnode = nodes.get(fchar);
					if (!fnode.isLast() && chs.length == 1)
						fnode.setLast(true);
				}
				lastIndex = chs.length - 1;
				for (int i = 1; i < chs.length; i++) {
					fnode = fnode.addIfNoExist(charConvert(chs[i]), i == lastIndex);
				}
			}
		}
	}

	/**
	 * 过滤判断 将敏感词转化为成屏蔽词
	 * 
	 * @param src
	 * @return
	 */
	public final String doFilter(final String src) {
        addSensitiveWord(this.getFilterKeywordAllList());
        //干扰符号选择文本方式加载，由于ehcache里特殊字符串不能缓存，所以选择了使用文本方式加载特殊字符串
        //addStopWord(readWordFromFile("stopwd.txt"));
		if (set != null && nodes != null) {
			char[] chs = src.toCharArray();
			int length = chs.length;
			int currc; // 当前检查的字符
			int cpcurrc; // 当前检查字符的备份
			int k;
			WordNode node;
			for (int i = 0; i < length; i++) {
				currc = charConvert(chs[i]);
				if (!set.contains(currc)) {
					continue;
				}
				node = nodes.get(currc);// 日 2
				if (node == null)// 其实不会发生，习惯性写上了
					continue;
				boolean couldMark = false;
				int markNum = -1;
				if (node.isLast()) {// 单字匹配（日）
					couldMark = true;
					markNum = 0;
				}
				// 继续匹配（日你/日你妹），以长的优先
				// 你-3 妹-4 夫-5
				k = i;
				cpcurrc = currc; // 当前字符的拷贝
				for (; ++k < length;) {
					int temp = charConvert(chs[k]);
					if (temp == cpcurrc)
						continue;
					if (stopwdSet != null && stopwdSet.contains(temp))
						continue;
					node = node.querySub(temp);
					if (node == null)// 没有了
						break;
					if (node.isLast()) {
						couldMark = true;
						markNum = k - i;// 3-2
					}
					cpcurrc = temp;
				}
				if (couldMark) {
					for (k = 0; k <= markNum; k++) {
						chs[k + i] = SIGN;
					}
					i = i + markNum;
				}
			}
			return new String(chs);
		}

		return src;
	}

	/**
	 * 是否包含敏感词
	 * 
	 * @param src
	 * @return
	 */
	public static final boolean isContains(final String src) {
		if (set != null && nodes != null) {
			char[] chs = src.toCharArray();
			int length = chs.length;
			int currc; // 当前检查的字符
			int cpcurrc; // 当前检查字符的备份
			int k;
			WordNode node;
			for (int i = 0; i < length; i++) {
				currc = charConvert(chs[i]);
				if (!set.contains(currc)) {
					continue;
				}
				node = nodes.get(currc);// 日 2
				if (node == null)// 其实不会发生，习惯性写上了
					continue;
				boolean couldMark = false;
				if (node.isLast()) {// 单字匹配（日）
					couldMark = true;
				}
				// 继续匹配（日你/日你妹），以长的优先
				// 你-3 妹-4 夫-5
				k = i;
				cpcurrc = currc;
				for (; ++k < length;) {
					int temp = charConvert(chs[k]);
					if (temp == cpcurrc)
						continue;
					if (stopwdSet != null && stopwdSet.contains(temp))
						continue;
					node = node.querySub(temp);
					if (node == null)// 没有了
						break;
					if (node.isLast()) {
						couldMark = true;
					}
					cpcurrc = temp;
				}
				if (couldMark) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 大写转化为小写 全角转化为半角
	 * 
	 * @param src
	 * @return
	 */
	private static int charConvert(char src) {
		int r = SymbolConvertUtils.qj2bj(src);
		return (r >= 'A' && r <= 'Z') ? r + 32 : r;
	}

	/**
	 * 判断一个集合是否为空
	 * 
	 * @param col
	 * @return
	 */
	public static <T> boolean isEmpty(final Collection<T> col) {
		if (col == null || col.isEmpty()) {
			return true;
		}
		return false;
	}
}
