var pluginId = null;
var namespace = null;

function bindExpand(e) {
    e.click(function() {
        var pageId = $(this).data('page-id');
        var listItem = $(this).closest('.list-group-item');
        var $this = $(this);
        $.ajax({
            method: 'get',
            url: '/api/projects/' + pluginId + '/pages?parentId=' + pageId,
            dataType: 'json',
            success: function(childPages) {
                console.log(childPages);
                var div = $('<div class="page-children" data-page-id="' + pageId + '"></div>');
                listItem.after(div);
                for (var i = 0; i < childPages.length; i++) {
                    var page = childPages[i];
                    var url = '/' + namespace + '/pages/' + page.name;
                    div.append(
                        '<li class="list-group-item page-item-child">' +
                        '<a href="' + url + '">' + page.name + '</a>' +
                        '</li>'
                    );
                }
                $this.removeClass('page-expand')
                    .addClass('page-collapse')
                    .find('i')
                    .removeClass('fa-plus-square-o')
                    .addClass('fa-minus-square-o');
                $this.off('click');
                bindCollapse($this);
            }
        })
    });
}

function bindCollapse(e) {
    e.click(function() {
        var pageId = $(this).data('page-id');
        $('.page-children[data-page-id="' + pageId + '"]').remove();
        $(this).removeClass('page-collapse')
            .addClass('page-expand')
            .find('i')
            .removeClass('fa-minus-square-o')
            .addClass('fa-plus-square-o');
        $(this).off('click');
        bindExpand($(this));
    });
}

$(function() {
    bindExpand($('.page-expand'));
    bindCollapse($('.page-collapse'));
});
