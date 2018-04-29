import javax.inject.Inject
import play.api.http.{DefaultHttpFilters, EnabledFilters}
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import security.NonceFilter

class Filters @Inject()(enabledFilters: EnabledFilters, csrfFilters: CSRFFilter, securityHeadersFilter: SecurityHeadersFilter, nonceFilter: NonceFilter)
  extends DefaultHttpFilters(Seq(csrfFilters, securityHeadersFilter, nonceFilter) ++ enabledFilters.filters : _*)
