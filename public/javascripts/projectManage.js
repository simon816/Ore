var projectName = null;

var KEY_RETURN = 13;

$(function() {
    var name = $('#name');
    name.on('input', function() {
        var val = $(this).val();
        $('#btn-rename').prop('disabled', val.length === 0 || val === projectName);
    });

    name.keydown(function(e) {
        if (e.which === KEY_RETURN) {
            e.preventDefault();
            $('#btn-rename').click();
        }
    });
});
