var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

$(function() {
    var form = $('#form-icon');
    var btn = form.find('button');
    btn.click(function(e) {
        var icon = btn.find('i').removeClass('fa-upload').addClass('fa-spinner fa-spin');
        e.preventDefault();
        var formData = new FormData(form[0]);
        var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/icon';
        $.ajax({
            url: url,
            type: 'post',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,
            complete: function() {
                form.find('img').attr('src', url + '/pending');
                icon.removeClass('fa-spinner fa-spin').addClass('fa-upload');
            }
        });
    });
});
