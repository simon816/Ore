var ICON_VIS = 'fa-eye-slash';
var ICON_INVIS = 'fa-eye';

$(function() {
    $('.btn-hide').click(function () {
        var lang = $(this).find('span');
        var icon = $(this).find('i');
        var visible = icon.hasClass(ICON_VIS);
        var iconClass = visible ? ICON_VIS : ICON_INVIS;
        var project = $(this).data('project');
        var spinner = icon.removeClass(iconClass).addClass('fa-spinner fa-spin');
        $.ajax({
            url: '/' + project + '/visible/' + !visible,
            fail: function () {
                spinner.addClass(iconClass).removeClass('fa-spinner fa-spin');
            },
            success: function () {
                spinner.addClass(visible ? ICON_INVIS : ICON_VIS).removeClass('fa-spinner fa-spin');
                lang.text(visible ? 'Unhide' : 'Hide');
            }
        });
    });
});
