-- Keep investigation context available beside each business disposition row.
CREATE OR REPLACE VIEW analytics.v_decisions AS
SELECT d.id AS decision_record_id, d.investigation_id, d.excursion_id,
       ex.shipment_id, d.decision AS decision_value, d.decided_at,
       d.rule_version, 'ANALYST'::text AS actor_role,
       ex.affected_lot_ids, inv.status AS investigation_status,
       inv.opened_at AS investigation_opened_at,
       inv.contradictory_evidence
FROM public.disposition_decisions d
JOIN analytics.v_excursions ex ON ex.excursion_id = d.excursion_id
JOIN analytics.v_investigations inv ON inv.investigation_id = d.investigation_id;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'coldchain_analytics') THEN
    GRANT SELECT ON analytics.v_decisions TO coldchain_analytics;
  END IF;
END $$;
