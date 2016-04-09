var MAX_FILE_SIZE = 1048576;

$(function() {
    $('#pluginFile').on('change', function() {
        var alert = $('.alert-file');
        var fileName = $(this).val().trim();
        var fileSize = this.files[0].size;
        if (!fileName) {
            alert.fadeOut(1000);
            return;
        }

        if (fileSize > MAX_FILE_SIZE) {
            alert.find('i').removeClass('fa-upload').addClass('fa-times');
            alert.find('.file-upload').find('button')
                .removeClass('btn-success')
                .addClass('btn-danger')
                .prop('disabled', true);
            alert.find('.file-size').css('color', '#d9534f');
        }

        fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
        alert.find('.file-name').text(fileName);
        alert.find('.file-size').text(filesize(this.files[0].size));
        alert.fadeIn('slow');
    });
});
