/**
 * @brief 地区联动areaSelect插件，设置child属性进行关联
 * 使用方式如下：
 * <select name="province" child="city,area" areaSelect></select>
 * <select name="city" child="area"></select>
 * <select name="area"></select>
 */
function areaSelect(topName)
{
	var _this    = this;
	this.topName = topName;
	this.child   = [];

	/**
	 * 生成地域js联动下拉框
	 * @param name
	 * @param parent_id
	 * @param select_id
	 */
	this.createAreaSelect = function(name,parent_id,select_id)
	{
		parent_id = parent_id ? parent_id : 0;

		//artTemplate模板 {name:组件名字,area_id:选中的地区ID,data:地区的对象}
		areaTemplate = '<option value="">请选择</option>'+'<%for(var index in data){%>'+'<%var item = data[index]%>'+'<option value="<%=item.area_id%>" <%if(item.area_id == select_id){%>selected="selected"<%}%>><%=item.area_name%></option>'+'<%}%>';

		//生成地区
		$.getJSON(creatUrl("block/area_child"),{"aid":parent_id,"random":Math.random()},function(json)
		{
			$('select[name="'+name+'"]').html( template.compile(areaTemplate)({"select_id":select_id,"data":json}) );
		});
	}

	/**
	 * @brief 联动菜单控件(SELECT)的onchange事件触发
	 * @param object _self 控件本身
	 */
	this.areaChangeCallback = function(_self)
	{
		var parent_id = $(_self).val();
		var childName = $(_self).attr('child');

		if(!childName)
		{
			return;
		}

		//拆分子对象
		var childArray = childName.split(',');
		for(var index in childArray)
		{
			$('select[name="'+childArray[index]+'"]').html('<option value="">请选择</option>');
		}

		//生成js联动菜单
		if(parent_id)
		{
			_this.createAreaSelect(childArray[0],parent_id);
		}
	}

	/**
	 * @brief 初始化init数据
	 * @param object initData 初始化数据
	 */
	this.init = function(initData)
	{
		$(function()
		{
			var lastParent = "";
			if(initData)
			{
				for(var c in _this.child)
				{
					for(var i in initData)
					{
						//匹配到数据
						if(_this.child[c] == i && initData[i])
						{
							_this.createAreaSelect(i,lastParent,initData[i]);
							lastParent = initData[i];
						}
					}
				}
			}

			if(!initData || !lastParent)
			{
				_this.createAreaSelect(_this.child[0]);
			}
		})
	}

	//构造函数
	$(function()
	{
		var topObj = $("select[name='"+topName+"']");
		if(topObj.length > 0)
		{
			//绑定事件
			var childString = topObj.attr('child');
			var childArray  = childString ? childString.split(",") : [];
			childArray.unshift(topObj.attr('name'));

			_this.child = childArray;
			if(childArray.length > 0)
			{
				for(var i in childArray)
				{
					$('select[name="'+childArray[i]+'"]').on("change",function(){_this.areaChangeCallback(this);});
				}
			}
		}
	})
}