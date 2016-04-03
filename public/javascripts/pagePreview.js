$(function() {
    $('[data-toggle="tab"]').click(function(event) {
        event.preventDefault();
        $(this).tab('show');
    });

    $('[href="#preview"]').on('shown.bs.tab', function() {
        $('#preview').html(markdown.toHTML($('#content').val()))
    });
});
