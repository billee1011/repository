$.extend($.fn.datagrid.defaults.editors, {  
    bfDateEdit : {
    init: function(container, options)
    {
        var editorContainer = $('<div/>');  
        
        var input = $("<input name=\"timeEditor\" readonly=\"readonly\" onfocus=\"WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})\">");
        var button = $("<img src='css/easyui_themes/icons/ok.png' class='rightElement' onclick='eval(" + options.savecallback + "(this, 1))'/>");  
        var cancelButton = $("<img src='css/easyui_themes/icons/undo.png' class='rightElement' onclick='eval(" + options.savecallback + "(this, 0))'/>");  
  
        editorContainer.append(input);
        editorContainer.append(button);  
        editorContainer.append(cancelButton);
        editorContainer.appendTo(container);
        return editorContainer;
    }, 
    getValue: function(target)  
    {  
        return $(target).find('input').text();  
    },  
    setValue: function(target, value)  
    {  
        $(target).find('input').val(value);
    },  
    resize: function(target, width)  
    {  
        var span = $(target);  
        if ($.boxModel == true){  
            span.width(width - (span.outerWidth() - span.width()) - 10);  
        } else {  
            span.width(width - 10);  
        }  
    }
    }  
});  