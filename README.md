# Praksis MCP

An [MCP](https://modelcontextprotocol.io) server for the [Praksis](https://github.com/tuhde/Praksis)
daemon, in Java. An agent (Claude Code, Claude Desktop, anything that speaks MCP) gets tools to look
at the daemon and, later, to propose changes. **It has no rights of its own:** the server is a client
of the daemon's web API like [Praksis-Web](https://github.com/tuhde/Praksis-Web), so it can do only
what the token you give it may do. Approvals, grants, pins, plan runs and layer updates stay with
you at the daemon, and no tool offers them.

**Status:** the read tools and the resources. Tools: `praksis_status`, `praksis_effective`,
`praksis_layers`, `praksis_rules_list`, `praksis_rule_read`, `praksis_history_find`,
`praksis_history_trace`, `praksis_history_summary`, `praksis_log`, `praksis_audit`,
`praksis_approvals_list`, `praksis_proposals_list`, `praksis_proposal_show`, `praksis_plans_list`,
`praksis_plan_show`, `praksis_help` and `praksis_verify` (a candidate ruleset, never with the option
that fetches). Also read: `praksis_plan_catalog`, `praksis_staged_list`, `praksis_staged_show`, `praksis_notes_list`.
Proposals (mode `propose` or `commands`, a token that is not read-only): `praksis_agent_propose`
(test files for a rule, staged), `praksis_plan_propose` (a plan that waits for you) and
`praksis_notes_save`. Nothing a proposal makes is applied, run or granted; each use is in the audit
log as `web:<token name>`. Resources: `praksis://help` (the pages), `praksis://help/<page>` and
`praksis://help/<section>/<page>` (a page in Markdown) and `praksis://effective`. The wrapper for text from outside and the limits follow (see `docs/PLAN.md` of Praksis,
"MCP server").

## Text from outside

Results hold text that strangers wrote (the subject, sender and body of a mail). So every result
except the status and the documentation is put between two markers with a random number in them,
after a line that says that what is inside is data and not instructions; text inside cannot close
the block, because the number is made after the text is known. A result longer than 20,000
characters is cut, with a note outside the markers; every such tool takes `max_chars` (1,000 to
200,000) to change that. The daemon never sees `max_chars`.

What the server can ask of the daemon is a short list (the questions of the read tools, `inspect`
with five questions, `verify` without the option that fetches, and `stage` with three kinds), and a
test sends every tool's request and compares it with that list and with a list of requests that
must never be sent (answering an approval, a grant, a plan run, a layer update, an entity write).
A new tool that sends something else fails the test until someone decides it should.

The daemon needs the requests `inspect` and `verify` for web tokens, so build it from a commit that
has them (the core at the commit that adds `stage` or later). The server waits when it has four requests under way,
because the daemon answers a ninth request at once of one token with 429.

## Try it

1. Turn on the web listener of the daemon (`[web] listen = "127.0.0.1:8484"` in `praksis.toml`) and
   restart it.
2. `praksis web token create claude --read-only` (shown once).
3. Build (the core must be installed first: `./mvnw install` in `../Praksis`):

       ./mvnw package

   and add the server to your client, for Claude Code:

       claude mcp add praksis --env PRAKSIS_TOKEN=pwt_... -- java -jar /path/to/Praksis-MCP/target/praksis-mcp.jar

## Settings (environment)

| Variable | Meaning |
|---|---|
| `PRAKSIS_TOKEN` | the web token (required; not accepted as an argument, so it is not in a process list) |
| `PRAKSIS_URL` | the address of the listener, default `http://127.0.0.1:8484` |
| `PRAKSIS_MODE` | `read-only` (default) offers only tools that look; `propose` also the tools that hand you a proposal; `commands` also tools that start commands. The daemon enforces the token whichever you choose, so this only decides what the client sees. |

One server per daemon, so one per identity. The log goes to standard error; standard output is the
protocol.

## Tests

`./mvnw verify` runs the tests: the configuration, the client of the daemon against a fake listener,
the tools by mode, the wrapper for text from outside, the list of what the server may ask of the
daemon, and the built program in its own process spoken to by a real MCP client.

It then runs `LiveDaemonIT` against the **real daemon** of the core: a mail with a hostile subject
is delivered to a Maildir, and an agent that does what the mail says finds the tool missing, is
refused by the daemon when its token is read-only, and cannot approve anything; a proposal from a
token that may make one is kept for the person and audited as `web:<token name>`. It needs the
runtime image of the core (`./mvnw package` in `../Praksis`, or `-Dpraksis.image=<dir>`); without
one the test is skipped and says so.

## License

Apache-2.0.
