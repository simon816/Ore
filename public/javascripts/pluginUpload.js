var MAX_FILE_SIZE = 20971520;

function getAlert() {
    return $('.alert-file');
}

function failure(message) {
    var alert = getAlert();
    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-success').addClass('btn-danger').prop('disabled', true);

    var bs = alert.find('.alert');
    bs.removeClass('alert-info').addClass('alert-danger');
    bs.find('i').removeClass('fa-file-archive-o').addClass('fa-exclamation-circle').tooltip({
        placement: 'left',
        title: message
    });
}

function reset() {
    var alert = getAlert();
    alert.hide();

    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-danger').addClass('btn-success').prop('disabled', false);

    var bs = alert.find('.alert');
    bs.removeClass('alert-danger').addClass('alert-info');
    bs.find('i').removeClass('fa-exclamation-circle').addClass('fa-file-archive-o').tooltip('destroy');

    return alert;
}

$(function() {
    $('#pluginFile').on('change', function() {
        var alert = reset();
        var fileName = $(this).val().trim();
        var fileSize = this.files[0].size;
        if (!fileName) {
            alert.fadeOut(1000);
            return;
        }

        if (fileSize > MAX_FILE_SIZE) {
            failure('That file is too big. Plugins may be no larger than ' + filesize(MAX_FILE_SIZE) + '.');
        } else if (!fileName.endsWith('.zip') && !fileName.endsWith('.jar')) {
            failure('Only JAR and ZIP files are accepted.');
        }

        fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
        alert.find('.file-name').text(fileName);
        alert.find('.file-size').text(filesize(this.files[0].size));
        alert.fadeIn('slow');
    });

    $('.file-upload').find('button').click(function() {
        $(this).find('i').removeClass('fa-upload').addClass('fa-spinner fa-spin');
    });
});
