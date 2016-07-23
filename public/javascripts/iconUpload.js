var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

$(function() {
    var form = $('#form-icon');
    var btn = form.find('button');
    var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/icon';
    var preview = form.find('img');
    var input = form.find('input');

    function updateButton() {
        btn.prop('disabled', input[0].files.length == 0);
    }

    input.on('change', function() { updateButton(); });

    // Upload button
    btn.click(function(e) {
        e.preventDefault();
        var icon = btn.find('i').removeClass('fa-upload').addClass('fa-spinner fa-spin');
        $.ajax({
            url: url,
            type: 'post',
            data: new FormData(form[0]),
            cache: false,
            contentType: false,
            processData: false,
            complete: function() {
                preview.attr('src', url + '/pending');
                icon.removeClass('fa-spinner fa-spin').addClass('fa-upload');
            },
            success: function() {
                $('#update-icon').val('true');
                input.val('');
                updateButton();
            }
        });
    });

    // Reset button
    var reset = form.find('.btn-reset');
    reset.click(function() {
        $(this).text('').append('<i class="fa fa-spinner fa-spin"></i>');
        $.ajax({
            url: url + '/reset',
            type: 'post',
            complete: function() {
                reset.empty().text('Reset');
            },
            success: function() {
                preview.attr('src', url);
                input.val('');
                updateButton();
            }
        });
    });
});
